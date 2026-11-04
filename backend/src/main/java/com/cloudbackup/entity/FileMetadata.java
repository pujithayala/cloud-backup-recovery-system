package com.cloudbackup.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_metadata", indexes = {
    @Index(name = "idx_files_user_id", columnList = "user_id"),
    @Index(name = "idx_files_is_deleted", columnList = "is_deleted")
})
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "original_path", nullable = false, length = 1024)
    private String originalPath = "/";

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType = "application/octet-stream";

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes = 0L;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FileMetadata() {
    }

    public FileMetadata(User user, String filename, String originalPath, String contentType, Long sizeBytes) {
        this.user = user;
        this.filename = filename;
        this.originalPath = (originalPath != null && !originalPath.isBlank()) ? originalPath : "/";
        this.contentType = (contentType != null && !contentType.isBlank()) ? contentType : "application/octet-stream";
        this.sizeBytes = (sizeBytes != null) ? sizeBytes : 0L;
        this.isDeleted = false;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.originalPath == null || this.originalPath.isBlank()) {
            this.originalPath = "/";
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

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean deleted) {
        isDeleted = deleted;
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

