package com.cloudbackup.dto.analytics;

public class DashboardStatsResponse {

    private long totalFilesBackedUp;
    private long totalStorageConsumedBytes;
    private long storageQuotaBytes;
    private double storageUsedPercentage;
    private long successfulBackups;
    private long failedBackups;
    private long totalRecoveryOperations;
    private long activeSchedules;
    private long unreadNotifications;

    public DashboardStatsResponse() {
    }

    public long getTotalFilesBackedUp() {
        return totalFilesBackedUp;
    }

    public void setTotalFilesBackedUp(long totalFilesBackedUp) {
        this.totalFilesBackedUp = totalFilesBackedUp;
    }

    public long getTotalStorageConsumedBytes() {
        return totalStorageConsumedBytes;
    }

    public void setTotalStorageConsumedBytes(long totalStorageConsumedBytes) {
        this.totalStorageConsumedBytes = totalStorageConsumedBytes;
    }

    public long getStorageQuotaBytes() {
        return storageQuotaBytes;
    }

    public void setStorageQuotaBytes(long storageQuotaBytes) {
        this.storageQuotaBytes = storageQuotaBytes;
    }

    public double getStorageUsedPercentage() {
        return storageUsedPercentage;
    }

    public void setStorageUsedPercentage(double storageUsedPercentage) {
        this.storageUsedPercentage = storageUsedPercentage;
    }

    public long getSuccessfulBackups() {
        return successfulBackups;
    }

    public void setSuccessfulBackups(long successfulBackups) {
        this.successfulBackups = successfulBackups;
    }

    public long getFailedBackups() {
        return failedBackups;
    }

    public void setFailedBackups(long failedBackups) {
        this.failedBackups = failedBackups;
    }

    public long getTotalRecoveryOperations() {
        return totalRecoveryOperations;
    }

    public void setTotalRecoveryOperations(long totalRecoveryOperations) {
        this.totalRecoveryOperations = totalRecoveryOperations;
    }

    public long getActiveSchedules() {
        return activeSchedules;
    }

    public void setActiveSchedules(long activeSchedules) {
        this.activeSchedules = activeSchedules;
    }

    public long getUnreadNotifications() {
        return unreadNotifications;
    }

    public void setUnreadNotifications(long unreadNotifications) {
        this.unreadNotifications = unreadNotifications;
    }
}
