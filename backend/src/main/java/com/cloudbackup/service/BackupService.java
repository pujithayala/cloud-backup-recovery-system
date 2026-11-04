package com.cloudbackup.service;

import com.cloudbackup.dto.backup.BackupDetailResponse;
import com.cloudbackup.dto.backup.BackupSummaryResponse;
import com.cloudbackup.dto.backup.BackupVersionResponse;
import com.cloudbackup.dto.backup.CreateBackupRequest;
import com.cloudbackup.entity.*;
import com.cloudbackup.exception.BadRequestException;
import com.cloudbackup.exception.ResourceNotFoundException;
import com.cloudbackup.repository.BackupRepository;
import com.cloudbackup.repository.BackupVersionRepository;
import com.cloudbackup.repository.FileMetadataRepository;
import com.cloudbackup.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private final BackupRepository backupRepository;
    private final BackupVersionRepository versionRepository;
    private final FileMetadataRepository fileRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final ChecksumService checksumService;
    private final NotificationService notificationService;

    public BackupService(BackupRepository backupRepository,
                         BackupVersionRepository versionRepository,
                         FileMetadataRepository fileRepository,
                         UserRepository userRepository,
                         StorageService storageService,
                         ChecksumService checksumService,
                         NotificationService notificationService) {
        this.backupRepository = backupRepository;
        this.versionRepository = versionRepository;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.checksumService = checksumService;
        this.notificationService = notificationService;
    }

    @Transactional
    public BackupDetailResponse createBackup(UUID userId, CreateBackupRequest request, BackupSchedule schedule) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<FileMetadata> files = fileRepository.findAllByIdInAndUserIdAndIsDeletedFalse(request.getFileIds(), userId);
        if (files.isEmpty()) {
            throw new BadRequestException("No valid files found for the provided file IDs");
        }

        Backup backup = new Backup(user, request.getBackupName(), request.getDescription());
        if (schedule != null) {
            backup.setSchedule(schedule);
        }
        backup.setStatus(BackupStatus.IN_PROGRESS);
        backup = backupRepository.save(backup);

        try {
            for (FileMetadata file : files) {
                Optional<BackupVersion> latestVersionOpt = versionRepository.findTopByFileIdOrderByVersionNumberDesc(file.getId());
                int nextVersion = latestVersionOpt.map(v -> v.getVersionNumber() + 1).orElse(1);

                String storageKey = String.format("users/%s/files/%s/v%d_%s",
                        userId.toString(), file.getId().toString(), nextVersion, file.getFilename());

                // Content snapshot simulation: In production, file input stream comes from persistent blob or upload cache
                String contentPayload = String.format("FILE_SNAPSHOT::[name=%s,size=%d,version=%d,timestamp=%s,path=%s]",
                        file.getFilename(), file.getSizeBytes(), nextVersion, Instant.now(), file.getOriginalPath());
                byte[] data = contentPayload.getBytes(StandardCharsets.UTF_8);
                long payloadSize = Math.max(file.getSizeBytes(), data.length);

                // Compute SHA-256 checksum
                String checksum = checksumService.calculateSha256(data);

                // Upload to MinIO/S3 object storage
                storageService.upload(storageKey, new ByteArrayInputStream(data), data.length, file.getContentType());

                // Create BackupVersion record
                BackupVersion version = new BackupVersion(backup, file, nextVersion, storageKey, checksum, payloadSize);
                backup.addVersion(version);
            }

            backup.setStatus(BackupStatus.COMPLETED);
            backup.setCompletedAt(Instant.now());
            Backup savedBackup = backupRepository.save(backup);

            notificationService.createNotification(
                    userId,
                    "Backup Completed Successfully",
                    String.format("Backup '%s' completed with %d file version(s) captured.", savedBackup.getBackupName(), savedBackup.getTotalFiles()),
                    NotificationType.BACKUP_SUCCESS,
                    "{\"backupId\":\"" + savedBackup.getId() + "\"}"
            );

            log.info("Backup {} successfully created for user {} with {} versions", savedBackup.getId(), userId, savedBackup.getTotalFiles());
            return BackupDetailResponse.fromEntity(savedBackup);

        } catch (Exception e) {
            log.error("Failed to execute backup '{}': {}", request.getBackupName(), e.getMessage(), e);
            backup.setStatus(BackupStatus.FAILED);
            backup.setCompletedAt(Instant.now());
            backupRepository.save(backup);

            notificationService.createNotification(
                    userId,
                    "Backup Failed",
                    String.format("Backup '%s' failed: %s", request.getBackupName(), e.getMessage()),
                    NotificationType.BACKUP_FAILURE,
                    "{\"backupId\":\"" + backup.getId() + "\"}"
            );

            throw new BadRequestException("Backup execution failed: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<BackupSummaryResponse> listBackups(UUID userId, BackupStatus status, Pageable pageable) {
        Page<Backup> page;
        if (status != null) {
            page = backupRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            page = backupRepository.findByUserId(userId, pageable);
        }
        return page.map(BackupSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public BackupDetailResponse getBackupById(UUID backupId, UUID userId) {
        Backup backup = backupRepository.findByIdAndUserId(backupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Backup", "id", backupId));
        return BackupDetailResponse.fromEntity(backup);
    }

    @Transactional
    public void deleteBackup(UUID backupId, UUID userId) {
        Backup backup = backupRepository.findByIdAndUserId(backupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Backup", "id", backupId));

        // Purge stored blobs from S3/MinIO
        for (BackupVersion version : backup.getVersions()) {
            try {
                if (storageService.exists(version.getStorageKey())) {
                    storageService.delete(version.getStorageKey());
                }
            } catch (Exception ex) {
                log.warn("Could not delete S3 blob for key {}: {}", version.getStorageKey(), ex.getMessage());
            }
        }

        backupRepository.delete(backup);
        log.info("Deleted backup {} for user {}", backupId, userId);
    }

    @Transactional(readOnly = true)
    public List<BackupVersionResponse> getVersionsForFile(UUID fileId, UUID userId) {
        return versionRepository.findByFileIdAndUserId(fileId, userId)
                .stream()
                .map(BackupVersionResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
