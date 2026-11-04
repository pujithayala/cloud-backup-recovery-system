package com.cloudbackup.dto.analytics;

public class ActivityPointDto {

    private String date;
    private int backupCount;
    private long bytesBackedUp;
    private int recoveryCount;

    public ActivityPointDto() {
    }

    public ActivityPointDto(String date, int backupCount, long bytesBackedUp, int recoveryCount) {
        this.date = date;
        this.backupCount = backupCount;
        this.bytesBackedUp = bytesBackedUp;
        this.recoveryCount = recoveryCount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getBackupCount() {
        return backupCount;
    }

    public void setBackupCount(int backupCount) {
        this.backupCount = backupCount;
    }

    public long getBytesBackedUp() {
        return bytesBackedUp;
    }

    public void setBytesBackedUp(long bytesBackedUp) {
        this.bytesBackedUp = bytesBackedUp;
    }

    public int getRecoveryCount() {
        return recoveryCount;
    }

    public void setRecoveryCount(int recoveryCount) {
        this.recoveryCount = recoveryCount;
    }
}
