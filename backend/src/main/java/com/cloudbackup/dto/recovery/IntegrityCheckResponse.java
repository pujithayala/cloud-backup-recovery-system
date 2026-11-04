package com.cloudbackup.dto.recovery;

import com.cloudbackup.entity.VersionStatus;

import java.time.Instant;
import java.util.UUID;

public class IntegrityCheckResponse {

    private UUID versionId;
    private String filename;
    private String storedChecksum;
    private String computedChecksum;
    private boolean match;
    private VersionStatus status;
    private Instant checkedAt;

    public IntegrityCheckResponse() {
        this.checkedAt = Instant.now();
    }

    public IntegrityCheckResponse(UUID versionId, String filename, String storedChecksum, String computedChecksum, boolean match, VersionStatus status) {
        this.versionId = versionId;
        this.filename = filename;
        this.storedChecksum = storedChecksum;
        this.computedChecksum = computedChecksum;
        this.match = match;
        this.status = status;
        this.checkedAt = Instant.now();
    }

    public UUID getVersionId() {
        return versionId;
    }

    public void setVersionId(UUID versionId) {
        this.versionId = versionId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getStoredChecksum() {
        return storedChecksum;
    }

    public void setStoredChecksum(String storedChecksum) {
        this.storedChecksum = storedChecksum;
    }

    public String getComputedChecksum() {
        return computedChecksum;
    }

    public void setComputedChecksum(String computedChecksum) {
        this.computedChecksum = computedChecksum;
    }

    public boolean isMatch() {
        return match;
    }

    public void setMatch(boolean match) {
        this.match = match;
    }

    public VersionStatus getStatus() {
        return status;
    }

    public VersionStatus getVersionStatus() {
        return status;
    }

    public void setStatus(VersionStatus status) {
        this.status = status;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }
}
