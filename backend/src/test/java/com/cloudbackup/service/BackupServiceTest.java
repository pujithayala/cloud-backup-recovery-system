package com.cloudbackup.service;

import com.cloudbackup.dto.backup.BackupDetailResponse;
import com.cloudbackup.dto.backup.CreateBackupRequest;
import com.cloudbackup.entity.*;
import com.cloudbackup.exception.BadRequestException;
import com.cloudbackup.repository.BackupRepository;
import com.cloudbackup.repository.BackupVersionRepository;
import com.cloudbackup.repository.FileMetadataRepository;
import com.cloudbackup.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock
    private BackupRepository backupRepository;

    @Mock
    private BackupVersionRepository versionRepository;

    @Mock
    private FileMetadataRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private ChecksumService checksumService;

    @Mock
    private NotificationService notificationService;

    private BackupService backupService;

    @BeforeEach
    void setUp() {
        backupService = new BackupService(
                backupRepository,
                versionRepository,
                fileRepository,
                userRepository,
                storageService,
                checksumService,
                notificationService
        );
    }

    @Test
    @DisplayName("Create backup snapshots files, computes SHA-256 and records version v1")
    void testCreateBackupSuccess() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@backup.io");

        UUID fileId = UUID.randomUUID();
        FileMetadata file = new FileMetadata(user, "database.dump", "/db", "application/octet-stream", 2048L);
        file.setId(fileId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileRepository.findAllByIdInAndUserIdAndIsDeletedFalse(List.of(fileId), userId))
                .thenReturn(List.of(file));

        when(versionRepository.findTopByFileIdOrderByVersionNumberDesc(fileId))
                .thenReturn(Optional.empty()); // No prior versions -> v1

        when(checksumService.calculateSha256(any(byte[].class)))
                .thenReturn("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890");

        when(backupRepository.save(any(Backup.class))).thenAnswer(invocation -> {
            Backup b = invocation.getArgument(0);
            if (b.getId() == null) {
                b.setId(UUID.randomUUID());
            }
            return b;
        });

        CreateBackupRequest request = new CreateBackupRequest("Daily DB Snapshot", "Manual snapshot", List.of(fileId));
        BackupDetailResponse response = backupService.createBackup(userId, request, null);

        assertNotNull(response);
        assertEquals(BackupStatus.COMPLETED, response.getStatus());
        assertEquals("Daily DB Snapshot", response.getBackupName());
        assertEquals(1, response.getTotalFiles());
        assertEquals(1, response.getVersions().size());
        assertEquals(1, response.getVersions().get(0).getVersionNumber());

        verify(storageService, times(1)).upload(anyString(), any(InputStream.class), anyLong(), anyString());
        verify(notificationService, times(1)).createNotification(eq(userId), anyString(), anyString(), eq(NotificationType.BACKUP_SUCCESS), anyString());
    }

    @Test
    @DisplayName("Create backup with existing version increments version number to v2")
    void testCreateBackupVersionIncrement() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        UUID fileId = UUID.randomUUID();
        FileMetadata file = new FileMetadata(user, "document.docx", "/work", "application/msword", 1024L);
        file.setId(fileId);

        Backup priorBackup = new Backup(user, "Prior", "Prior desc");
        BackupVersion existingV1 = new BackupVersion(priorBackup, file, 1, "storage/key/v1", "hash1", 1024L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileRepository.findAllByIdInAndUserIdAndIsDeletedFalse(List.of(fileId), userId))
                .thenReturn(List.of(file));
        when(versionRepository.findTopByFileIdOrderByVersionNumberDesc(fileId))
                .thenReturn(Optional.of(existingV1)); // latest is v1 -> next will be v2

        when(checksumService.calculateSha256(any(byte[].class))).thenReturn("sha256-v2-hash");
        when(backupRepository.save(any(Backup.class))).thenAnswer(invocation -> {
            Backup b = invocation.getArgument(0);
            if (b.getId() == null) b.setId(UUID.randomUUID());
            return b;
        });

        CreateBackupRequest request = new CreateBackupRequest("Incremental Backup", "Doc update", List.of(fileId));
        BackupDetailResponse response = backupService.createBackup(userId, request, null);

        assertNotNull(response);
        assertEquals(2, response.getVersions().get(0).getVersionNumber());
    }

    @Test
    @DisplayName("Create backup fails if file list is empty")
    void testCreateBackupEmptyFiles() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileRepository.findAllByIdInAndUserIdAndIsDeletedFalse(anyList(), eq(userId))).thenReturn(List.of());

        CreateBackupRequest request = new CreateBackupRequest("Empty", "Desc", List.of(UUID.randomUUID()));

        assertThrows(BadRequestException.class, () -> backupService.createBackup(userId, request, null));
    }
}

