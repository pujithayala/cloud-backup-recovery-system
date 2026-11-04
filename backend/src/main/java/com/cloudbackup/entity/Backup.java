package com.cloudbackup.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "backups", indexes = {
    @Index(name = "idx_backups_user_id", columnList = "user_id"),
    @Index(name = "idx_backups_status", columnList = "status")
})
public class Backup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private BackupSchedule schedule;

    @Column(name = "backup_name", nullable = false, length = 200)
    private String backupName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private BackupStatus status = BackupStatus.PENDING;

    @Column(name = "total_files", nullable = false)
    private Integer totalFiles = 0;

    @Column(name = "total_size_bytes", nullable = false)
    private Long totalSizeBytes = 0L;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "backup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BackupVersion> versions = new ArrayList<>();

    public Backup() {
    }

    public Backup(User user, String backupName, String description) {
        this.user = user;
        this.backupName = backupName;
        this.description = description;
        this.status = BackupStatus.PENDING;
        this.totalFiles = 0;
        this.totalSizeBytes = 0L;
    }

    @PrePersist
    protected void onCreate() {
        this.startedAt = Instant.now();
        if (this.status == null) {
            this.status = BackupStatus.PENDING;
        }
        if (this.totalFiles == null) {
            this.totalFiles = 0;
        }
        if (this.totalSizeBytes == null) {
            this.totalSizeBytes = 0L;
        }
    }

    public void addVersion(BackupVersion version) {
        versions.add(version);
        version.setBackup(this);
        this.totalFiles = versions.size();
        this.totalSizeBytes += (version.getSizeBytes() != null ? version.getSizeBytes() : 0L);
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

    public BackupSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(BackupSchedule schedule) {
        this.schedule = schedule;
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

    public List<BackupVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<BackupVersion> versions) {
        this.versions = versions;
    }
}
