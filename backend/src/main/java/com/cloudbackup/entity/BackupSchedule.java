package com.cloudbackup.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "backup_schedules", indexes = {
    @Index(name = "idx_schedules_user_id", columnList = "user_id")
})
public class BackupSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "schedule_name", nullable = false, length = 150)
    private String scheduleName;

    @Column(name = "cron_expression", nullable = false, length = 100)
    private String cronExpression;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 50)
    private ScheduleFrequency frequency = ScheduleFrequency.DAILY;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "target_directory", length = 1024)
    private String targetDirectory = "/";

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BackupSchedule() {
    }

    public BackupSchedule(User user, String scheduleName, String cronExpression, ScheduleFrequency frequency, String targetDirectory) {
        this.user = user;
        this.scheduleName = scheduleName;
        this.cronExpression = cronExpression;
        this.frequency = frequency != null ? frequency : ScheduleFrequency.DAILY;
        this.targetDirectory = (targetDirectory != null && !targetDirectory.isBlank()) ? targetDirectory : "/";
        this.isActive = true;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.targetDirectory == null) {
            this.targetDirectory = "/";
        }
        if (this.frequency == null) {
            this.frequency = ScheduleFrequency.DAILY;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
