package com.cloudbackup.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "backup_versions",
    indexes = {
        @Index(name = "idx_versions_file_id", columnList = "file_id"),
        @Index(name = "idx_versions_backup_id", columnList = "backup_id"),
        @Index(name = "idx_versions_checksum", columnList = "checksum_sha256")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_file_version", columnNames = {"file_id", "version_number"})
    }
)
public class BackupVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backup_id", nullable = false)
    private Backup backup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadata file;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Column(name = "storage_key", nullable = false, unique = true, length = 512)
    private String storageKey;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private VersionStatus status = VersionStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public BackupVersion() {
    }

    public BackupVersion(Backup backup, FileMetadata file, Integer versionNumber, String storageKey, String checksumSha256, Long sizeBytes) {
        this.backup = backup;
        this.file = file;
        this.versionNumber = versionNumber;
        this.storageKey = storageKey;
        this.checksumSha256 = checksumSha256;
        this.sizeBytes = sizeBytes;
        this.status = VersionStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.status == null) {
            this.status = VersionStatus.ACTIVE;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Backup getBackup() {
        return backup;
    }

    public void setBackup(Backup backup) {
        this.backup = backup;
    }

    public FileMetadata getFile() {
        return file;
    }

    public void setFile(FileMetadata file) {
        this.file = file;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public VersionStatus getStatus() {
        return status;
    }

    public void setStatus(VersionStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
