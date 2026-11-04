package com.cloudbackup.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recovery_logs", indexes = {
    @Index(name = "idx_recovery_user_id", columnList = "user_id"),
    @Index(name = "idx_recovery_version_id", columnList = "version_id")
})
public class RecoveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backup_id")
    private Backup backup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private BackupVersion version;

    @Column(name = "destination_path", length = 1024)
    private String destinationPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RecoveryStatus status = RecoveryStatus.INITIATED;

    @Column(name = "checksum_verified", nullable = false)
    private Boolean checksumVerified = false;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "recovered_at", nullable = false, updatable = false)
    private Instant recoveredAt;

    public RecoveryLog() {
    }

    public RecoveryLog(User user, Backup backup, BackupVersion version, String destinationPath) {
        this.user = user;
        this.backup = backup;
        this.version = version;
        this.destinationPath = destinationPath;
        this.status = RecoveryStatus.INITIATED;
        this.checksumVerified = false;
    }

    @PrePersist
    protected void onCreate() {
        this.recoveredAt = Instant.now();
        if (this.checksumVerified == null) {
            this.checksumVerified = false;
        }
        if (this.status == null) {
            this.status = RecoveryStatus.INITIATED;
        }
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

    public Backup getBackup() {
        return backup;
    }

    public void setBackup(Backup backup) {
        this.backup = backup;
    }

    public BackupVersion getVersion() {
        return version;
    }

    public void setVersion(BackupVersion version) {
        this.version = version;
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
