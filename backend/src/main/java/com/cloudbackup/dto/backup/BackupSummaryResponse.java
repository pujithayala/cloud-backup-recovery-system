package com.cloudbackup.dto.backup;

import com.cloudbackup.entity.Backup;
import com.cloudbackup.entity.BackupStatus;

import java.time.Instant;
import java.util.UUID;

public class BackupSummaryResponse {

    private UUID id;
    private String backupName;
    private String description;
    private BackupStatus status;
    private Integer totalFiles;
    private Long totalSizeBytes;
    private Instant startedAt;
    private Instant completedAt;

    public BackupSummaryResponse() {
    }

    public static BackupSummaryResponse fromEntity(Backup entity) {
        BackupSummaryResponse dto = new BackupSummaryResponse();
        dto.setId(entity.getId());
        dto.setBackupName(entity.getBackupName());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setTotalFiles(entity.getTotalFiles());
        dto.setTotalSizeBytes(entity.getTotalSizeBytes());
        dto.setStartedAt(entity.getStartedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBackupName() {
        return backupName;
    }

    public void setBackupName(String backupName) {
        this.backupName = backupName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BackupStatus getStatus() {
        return status;
    }

    public void setStatus(BackupStatus status) {
        this.status = status;
    }

    public Integer getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(Integer totalFiles) {
        this.totalFiles = totalFiles;
    }

    public Long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public void setTotalSizeBytes(Long totalSizeBytes) {
        this.totalSizeBytes = totalSizeBytes;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
