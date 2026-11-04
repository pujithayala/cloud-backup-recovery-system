package com.cloudbackup.dto.file;

import com.cloudbackup.entity.FileMetadata;

import java.time.Instant;
import java.util.UUID;

public class FileMetadataResponse {

    private UUID id;
    private String filename;
    private String originalPath;
    private String contentType;
    private Long sizeBytes;
    private Boolean isDeleted;
    private Instant createdAt;
    private Instant updatedAt;

    public FileMetadataResponse() {
    }

    public static FileMetadataResponse fromEntity(FileMetadata entity) {
        FileMetadataResponse dto = new FileMetadataResponse();
        dto.setId(entity.getId());
        dto.setFilename(entity.getFilename());
        dto.setOriginalPath(entity.getOriginalPath());
        dto.setContentType(entity.getContentType());
        dto.setSizeBytes(entity.getSizeBytes());
        dto.setIsDeleted(entity.getIsDeleted());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

