package com.cloudbackup.dto.schedule;

import com.cloudbackup.entity.BackupSchedule;
import com.cloudbackup.entity.ScheduleFrequency;

import java.time.Instant;
import java.util.UUID;

public class ScheduleResponse {

    private UUID id;
    private String scheduleName;
    private String cronExpression;
    private ScheduleFrequency frequency;
    private Boolean isActive;
    private String targetDirectory;
    private Instant lastRunAt;
    private Instant nextRunAt;
    private Instant createdAt;

    public ScheduleResponse() {
    }

    public static ScheduleResponse fromEntity(BackupSchedule entity) {
        ScheduleResponse dto = new ScheduleResponse();
        dto.setId(entity.getId());
        dto.setScheduleName(entity.getScheduleName());
        dto.setCronExpression(entity.getCronExpression());
        dto.setFrequency(entity.getFrequency());
        dto.setIsActive(entity.getIsActive());
        dto.setTargetDirectory(entity.getTargetDirectory());
        dto.setLastRunAt(entity.getLastRunAt());
        dto.setNextRunAt(entity.getNextRunAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getScheduleName() {
        return scheduleName;
    }

    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public ScheduleFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(ScheduleFrequency frequency) {
        this.frequency = frequency;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(Instant lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public Instant getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(Instant nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
