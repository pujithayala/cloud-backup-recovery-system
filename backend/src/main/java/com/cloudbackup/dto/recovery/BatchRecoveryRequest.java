package com.cloudbackup.dto.recovery;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public class BatchRecoveryRequest {

    @NotEmpty(message = "At least one version ID must be specified for recovery")
    private List<UUID> versionIds;

    private String destinationPath = "/recovered";

    public BatchRecoveryRequest() {
    }

    public BatchRecoveryRequest(List<UUID> versionIds, String destinationPath) {
        this.versionIds = versionIds;
        this.destinationPath = destinationPath;
    }

    public List<UUID> getVersionIds() {
        return versionIds;
    }

    public void setVersionIds(List<UUID> versionIds) {
        this.versionIds = versionIds;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }
}
