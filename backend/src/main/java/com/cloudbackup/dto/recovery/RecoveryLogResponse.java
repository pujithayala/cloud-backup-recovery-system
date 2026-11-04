package com.cloudbackup.dto.recovery;

import com.cloudbackup.entity.RecoveryLog;
import com.cloudbackup.entity.RecoveryStatus;

import java.time.Instant;
import java.util.UUID;

public class RecoveryLogResponse {

    private UUID id;
    private UUID backupId;
    private String backupName;
    private UUID versionId;
    private Integer versionNumber;
    private String filename;
    private String destinationPath;
    private RecoveryStatus status;
    private Boolean checksumVerified;
    private String errorMessage;
    private Instant recoveredAt;

    public RecoveryLogResponse() {
    }

    public static RecoveryLogResponse fromEntity(RecoveryLog entity) {
        RecoveryLogResponse dto = new RecoveryLogResponse();
        dto.setId(entity.getId());
        if (entity.getBackup() != null) {
            dto.setBackupId(entity.getBackup().getId());
            dto.setBackupName(entity.getBackup().getBackupName());
        }
        if (entity.getVersion() != null) {
            dto.setVersionId(entity.getVersion().getId());
            dto.setVersionNumber(entity.getVersion().getVersionNumber());
            if (entity.getVersion().getFile() != null) {
                dto.setFilename(entity.getVersion().getFile().getFilename());
            }
        }
        dto.setDestinationPath(entity.getDestinationPath());
        dto.setStatus(entity.getStatus());
        dto.setChecksumVerified(entity.getChecksumVerified());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setRecoveredAt(entity.getRecoveredAt());
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

    public UUID getVersionId() {
        return versionId;
    }

    public void setVersionId(UUID versionId) {
        this.versionId = versionId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public RecoveryStatus getStatus() {
        return status;
    }

    public void setStatus(RecoveryStatus status) {
        this.status = status;
    }

    public Boolean getChecksumVerified() {
        return checksumVerified;
    }

    public Boolean isChecksumVerified() {
        return checksumVerified;
    }

    public void setChecksumVerified(Boolean checksumVerified) {
        this.checksumVerified = checksumVerified;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getRecoveredAt() {
        return recoveredAt;
    }

    public void setRecoveredAt(Instant recoveredAt) {
        this.recoveredAt = recoveredAt;
    }
}
