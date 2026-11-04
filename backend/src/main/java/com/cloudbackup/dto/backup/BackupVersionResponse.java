package com.cloudbackup.dto.backup;

import com.cloudbackup.entity.BackupVersion;
import com.cloudbackup.entity.VersionStatus;

import java.time.Instant;
import java.util.UUID;

public class BackupVersionResponse {

    private UUID id;
    private UUID backupId;
    private String backupName;
    private UUID fileId;
    private String filename;
    private Integer versionNumber;
    private String storageKey;
    private String checksumSha256;
    private Long sizeBytes;
    private VersionStatus status;
    private Instant createdAt;

    public BackupVersionResponse() {
    }

    public static BackupVersionResponse fromEntity(BackupVersion entity) {
        BackupVersionResponse dto = new BackupVersionResponse();
        dto.setId(entity.getId());
        dto.setBackupId(entity.getBackup().getId());
        dto.setBackupName(entity.getBackup().getBackupName());
        dto.setFileId(entity.getFile().getId());
        dto.setFilename(entity.getFile().getFilename());
        dto.setVersionNumber(entity.getVersionNumber());
        dto.setStorageKey(entity.getStorageKey());
        dto.setChecksumSha256(entity.getChecksumSha256());
        dto.setSizeBytes(entity.getSizeBytes());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBackupId() {
        return backupId;
    }

    public void setBackupId(UUID backupId) {
        this.backupId = backupId;
    }

    public String getBackupName() {
        return backupName;
    }

    public void setBackupName(String backupName) {
        this.backupName = backupName;
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public VersionStatus getStatus() {
        return status;
    }

    public void setStatus(VersionStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
