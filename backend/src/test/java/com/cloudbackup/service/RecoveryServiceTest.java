package com.cloudbackup.service;

import com.cloudbackup.dto.recovery.IntegrityCheckResponse;
import com.cloudbackup.dto.recovery.RecoveryLogResponse;
import com.cloudbackup.entity.*;
import com.cloudbackup.exception.IntegrityViolationException;
import com.cloudbackup.repository.BackupVersionRepository;
import com.cloudbackup.repository.FileMetadataRepository;
import com.cloudbackup.repository.RecoveryLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecoveryServiceTest {

    @Mock
    private BackupVersionRepository versionRepository;

    @Mock
    private FileMetadataRepository fileRepository;

    @Mock
    private RecoveryLogRepository recoveryLogRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private ChecksumService checksumService;

    @Mock
    private NotificationService notificationService;

    private RecoveryService recoveryService;

    @BeforeEach
    void setUp() {
        recoveryService = new RecoveryService(
                versionRepository,
                fileRepository,
                recoveryLogRepository,
                storageService,
                checksumService,
                notificationService
        );
    }

    @Test
    @DisplayName("Verify integrity matches when stored hash equals computed hash")
    void testVerifyIntegritySuccess() {
        UUID userId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        FileMetadata file = new FileMetadata(user, "financials.xlsx", "/docs", "application/vnd.ms-excel", 5000L);
        Backup backup = new Backup(user, "Backup 1", "desc");
        String knownHash = "a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890";

        BackupVersion version = new BackupVersion(backup, file, 1, "storage/key/1", knownHash, 5000L);
        version.setId(versionId);

        when(versionRepository.findByIdAndUserId(versionId, userId)).thenReturn(Optional.of(version));
        when(storageService.download("storage/key/1")).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(checksumService.calculateSha256(any(InputStream.class))).thenReturn(knownHash);

        IntegrityCheckResponse result = recoveryService.verifyIntegrity(versionId, userId);

        assertNotNull(result);
        assertTrue(result.isMatch());
        assertEquals(knownHash, result.getStoredChecksum());
        assertEquals(knownHash, result.getComputedChecksum());
        assertEquals(VersionStatus.ACTIVE, result.getStatus());
    }

    @Test
    @DisplayName("Verify integrity detects data corruption, marks version as CORRUPTED and alerts")
    void testVerifyIntegrityCorrupted() {
        UUID userId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        FileMetadata file = new FileMetadata(user, "critical.db", "/data", "application/octet-stream", 8000L);
        Backup backup = new Backup(user, "Backup 2", "desc");
        String originalHash = "expected_hash_0000000000000000000000000000000000000000000000000000";
        String corruptedHash = "tampered_hash_1111111111111111111111111111111111111111111111111111";

        BackupVersion version = new BackupVersion(backup, file, 1, "storage/key/2", originalHash, 8000L);
        version.setId(versionId);

        when(versionRepository.findByIdAndUserId(versionId, userId)).thenReturn(Optional.of(version));
        when(storageService.download("storage/key/2")).thenReturn(new ByteArrayInputStream("tampered".getBytes()));
        when(checksumService.calculateSha256(any(InputStream.class))).thenReturn(corruptedHash);

        IntegrityCheckResponse result = recoveryService.verifyIntegrity(versionId, userId);

        assertNotNull(result);
        assertFalse(result.isMatch());
        assertEquals(VersionStatus.CORRUPTED, version.getStatus());
        verify(versionRepository, times(1)).save(version);
        verify(notificationService, times(1)).createNotification(
                eq(userId), contains("Corrupted"), anyString(), eq(NotificationType.SYSTEM_ALERT), anyString()
        );
    }

    @Test
    @DisplayName("Restore version succeeds when SHA-256 integrity check passes")
    void testRestoreVersionSuccess() {
        UUID userId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        FileMetadata file = new FileMetadata(user, "report.pdf", "/docs", "application/pdf", 1200L);
        file.setIsDeleted(true); // Was deleted
        Backup backup = new Backup(user, "Backup 3", "desc");
        String validHash = "valid_sha256_hash_here_1234567890abcdef1234567890abcdef12345678";

        BackupVersion version = new BackupVersion(backup, file, 1, "storage/key/3", validHash, 1200L);
        version.setId(versionId);

        when(versionRepository.findByIdAndUserId(versionId, userId)).thenReturn(Optional.of(version));
        when(storageService.download("storage/key/3")).thenReturn(new ByteArrayInputStream("valid payload".getBytes()));
        when(checksumService.calculateSha256(any(InputStream.class))).thenReturn(validHash);

        when(recoveryLogRepository.save(any(RecoveryLog.class))).thenAnswer(invocation -> {
            RecoveryLog log = invocation.getArgument(0);
            log.setId(UUID.randomUUID());
            return log;
        });

        RecoveryLogResponse response = recoveryService.restoreVersion(versionId, userId, "/recovered/files");

        assertNotNull(response);
        assertEquals(RecoveryStatus.SUCCESS, response.getStatus());
        assertTrue(response.getChecksumVerified());
        assertFalse(file.getIsDeleted(), "Deleted file should be undeleted upon restore");
        assertEquals(VersionStatus.RESTORED, version.getStatus());

        verify(fileRepository, times(1)).save(file);
        verify(notificationService, times(1)).createNotification(
                eq(userId), contains("Recovered"), anyString(), eq(NotificationType.RECOVERY_SUCCESS), anyString()
        );
    }

    @Test
    @DisplayName("Restore version aborts with IntegrityViolationException if file is corrupted")
    void testRestoreVersionAbortsOnCorruption() {
        UUID userId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        FileMetadata file = new FileMetadata(user, "archive.tar", "/data", "application/x-tar", 9000L);
        Backup backup = new Backup(user, "Backup 4", "desc");
        BackupVersion version = new BackupVersion(backup, file, 1, "storage/key/4", "hash_one", 9000L);
        version.setId(versionId);

        when(versionRepository.findByIdAndUserId(versionId, userId)).thenReturn(Optional.of(version));
        when(storageService.download("storage/key/4")).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(checksumService.calculateSha256(any(InputStream.class))).thenReturn("different_hash");

        assertThrows(IntegrityViolationException.class, () ->
                recoveryService.restoreVersion(versionId, userId, "/restored")
        );

        verify(recoveryLogRepository, times(1)).save(any(RecoveryLog.class));
        verify(notificationService, times(1)).createNotification(
                eq(userId), contains("Recovery Aborted"), anyString(), eq(NotificationType.RECOVERY_FAILURE), anyString()
        );
    }
}

