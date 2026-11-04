package com.cloudbackup.service;

import com.cloudbackup.dto.recovery.BatchRecoveryRequest;
import com.cloudbackup.dto.recovery.IntegrityCheckResponse;
import com.cloudbackup.dto.recovery.RecoveryLogResponse;
import com.cloudbackup.entity.*;
import com.cloudbackup.exception.IntegrityViolationException;
import com.cloudbackup.exception.ResourceNotFoundException;
import com.cloudbackup.repository.BackupVersionRepository;
import com.cloudbackup.repository.FileMetadataRepository;
import com.cloudbackup.repository.RecoveryLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RecoveryService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryService.class);

    private final BackupVersionRepository versionRepository;
    private final FileMetadataRepository fileRepository;
    private final RecoveryLogRepository recoveryLogRepository;
    private final StorageService storageService;
    private final ChecksumService checksumService;
    private final NotificationService notificationService;

    public RecoveryService(BackupVersionRepository versionRepository,
                           FileMetadataRepository fileRepository,
                           RecoveryLogRepository recoveryLogRepository,
                           StorageService storageService,
                           ChecksumService checksumService,
                           NotificationService notificationService) {
        this.versionRepository = versionRepository;
        this.fileRepository = fileRepository;
        this.recoveryLogRepository = recoveryLogRepository;
        this.storageService = storageService;
        this.checksumService = checksumService;
        this.notificationService = notificationService;
    }

    @Transactional
    public IntegrityCheckResponse verifyIntegrity(UUID versionId, UUID userId) {
        BackupVersion version = versionRepository.findByIdAndUserId(versionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("BackupVersion", "id", versionId));

        try (InputStream stream = storageService.download(version.getStorageKey())) {
            String computedChecksum = checksumService.calculateSha256(stream);
            boolean isMatch = computedChecksum.equalsIgnoreCase(version.getChecksumSha256());

            if (!isMatch) {
                version.setStatus(VersionStatus.CORRUPTED);
                versionRepository.save(version);

                notificationService.createNotification(
                        userId,
                        "Integrity Warning: Corrupted Backup File",
                        String.format("File '%s' version #%d has mismatched checksums. Data corruption detected.",
                                version.getFile().getFilename(), version.getVersionNumber()),
                        NotificationType.SYSTEM_ALERT,
                        "{\"versionId\":\"" + versionId + "\"}"
                );
            }

            return new IntegrityCheckResponse(
                    version.getId(),
                    version.getFile().getFilename(),
                    version.getChecksumSha256(),
                    computedChecksum,
                    isMatch,
                    version.getStatus()
            );
        } catch (Exception e) {
            log.error("Failed to execute integrity check for version {}: {}", versionId, e.getMessage(), e);
            throw new IntegrityViolationException("Could not verify file integrity from cloud storage: " + e.getMessage(), e);
        }
    }

    @Transactional
    public RecoveryLogResponse restoreVersion(UUID versionId, UUID userId, String destinationPath) {
        BackupVersion version = versionRepository.findByIdAndUserId(versionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("BackupVersion", "id", versionId));

        User user = version.getBackup().getUser();
        RecoveryLog recoveryLog = new RecoveryLog(user, version.getBackup(), version, destinationPath != null ? destinationPath : "/recovered");

        // 1. Mandatory SHA-256 Checksum Verification
        IntegrityCheckResponse checkResult = verifyIntegrity(versionId, userId);
        if (!checkResult.isMatch()) {
            recoveryLog.setStatus(RecoveryStatus.INTEGRITY_FAILED);
            recoveryLog.setChecksumVerified(false);
            recoveryLog.setErrorMessage(String.format("SHA-256 hash mismatch! Stored: %s, Computed: %s",
                    checkResult.getStoredChecksum(), checkResult.getComputedChecksum()));
            recoveryLogRepository.save(recoveryLog);

            notificationService.createNotification(
                    userId,
                    "Recovery Aborted",
                    String.format("Restoration of '%s' (v%d) was aborted due to integrity failure.",
                            version.getFile().getFilename(), version.getVersionNumber()),
                    NotificationType.RECOVERY_FAILURE,
                    "{\"versionId\":\"" + versionId + "\"}"
            );

            throw new IntegrityViolationException("Recovery aborted: File integrity validation failed for version " + version.getVersionNumber());
        }

        // 2. Perform restoration logic
        try {
            FileMetadata file = version.getFile();
            if (Boolean.TRUE.equals(file.getIsDeleted())) {
                file.setIsDeleted(false);
            }
            file.setSizeBytes(version.getSizeBytes());
            fileRepository.save(file);

            version.setStatus(VersionStatus.RESTORED);
            versionRepository.save(version);

            recoveryLog.setChecksumVerified(true);
            recoveryLog.setStatus(RecoveryStatus.SUCCESS);
            RecoveryLog savedLog = recoveryLogRepository.save(recoveryLog);

            notificationService.createNotification(
                    userId,
                    "File Recovered Successfully",
                    String.format("File '%s' successfully restored to version #%d.", file.getFilename(), version.getVersionNumber()),
                    NotificationType.RECOVERY_SUCCESS,
                    "{\"versionId\":\"" + versionId + "\",\"fileId\":\"" + file.getId() + "\"}"
            );

            log.info("Successfully recovered file {} version {} for user {}", file.getId(), version.getVersionNumber(), userId);
            return RecoveryLogResponse.fromEntity(savedLog);

        } catch (Exception ex) {
            recoveryLog.setStatus(RecoveryStatus.FAILED);
            recoveryLog.setErrorMessage("Restoration failure: " + ex.getMessage());
            recoveryLogRepository.save(recoveryLog);

            notificationService.createNotification(
                    userId,
                    "Recovery Failed",
                    String.format("Failed to restore '%s': %s", version.getFile().getFilename(), ex.getMessage()),
                    NotificationType.RECOVERY_FAILURE,
                    "{\"versionId\":\"" + versionId + "\"}"
            );

            throw new RuntimeException("Recovery failed: " + ex.getMessage(), ex);
        }
    }

    @Transactional
    public List<RecoveryLogResponse> batchRestore(UUID userId, BatchRecoveryRequest request) {
        List<RecoveryLogResponse> results = new ArrayList<>();
        for (UUID versionId : request.getVersionIds()) {
            results.add(restoreVersion(versionId, userId, request.getDestinationPath()));
        }
        return results;
    }

    @Transactional(readOnly = true)
    public Page<RecoveryLogResponse> getRecoveryHistory(UUID userId, Pageable pageable) {
        return recoveryLogRepository.findByUserIdOrderByRecoveredAtDesc(userId, pageable)
                .map(RecoveryLogResponse::fromEntity);
    }
}
