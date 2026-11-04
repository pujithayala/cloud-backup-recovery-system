package com.cloudbackup.dto.backup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public class CreateBackupRequest {

    @NotBlank(message = "Backup name is required")
    @Size(min = 2, max = 200, message = "Backup name must be between 2 and 200 characters")
    private String backupName;

    private String description;

    @NotEmpty(message = "At least one file must be selected for backup")
    private List<UUID> fileIds;

    public CreateBackupRequest() {
    }

    public CreateBackupRequest(String backupName, String description, List<UUID> fileIds) {
        this.backupName = backupName;
        this.description = description;
        this.fileIds = fileIds;
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

    public List<UUID> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<UUID> fileIds) {
        this.fileIds = fileIds;
    }
}
