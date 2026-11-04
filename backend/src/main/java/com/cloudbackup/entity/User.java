package com.cloudbackup.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Role role = Role.ROLE_USER;

    @Column(name = "storage_quota_bytes", nullable = false)
    private Long storageQuotaBytes = 10737418240L; // 10 GB default

    @Column(name = "storage_used_bytes", nullable = false)
    private Long storageUsedBytes = 0L;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public User() {
    }

    public User(String email, String passwordHash, String fullName, Role role, Long storageQuotaBytes) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = (role != null) ? role : Role.ROLE_USER;
        this.storageQuotaBytes = (storageQuotaBytes != null) ? storageQuotaBytes : 10737418240L;
        this.storageUsedBytes = 0L;
        this.isActive = true;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.storageQuotaBytes == null) {
            this.storageQuotaBytes = 10737418240L;
        }
        if (this.storageUsedBytes == null) {
            this.storageUsedBytes = 0L;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.role == null) {
            this.role = Role.ROLE_USER;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean canStoreAdditional(long additionalBytes) {
        return (this.storageUsedBytes + additionalBytes) <= this.storageQuotaBytes;
    }

    public void addStorageUsage(long bytes) {
        this.storageUsedBytes = Math.max(0L, this.storageUsedBytes + bytes);
    }

    public void reduceStorageUsage(long bytes) {
        this.storageUsedBytes = Math.max(0L, this.storageUsedBytes - bytes);
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getStorageQuotaBytes() {
        return storageQuotaBytes;
    }

    public void setStorageQuotaBytes(Long storageQuotaBytes) {
        this.storageQuotaBytes = storageQuotaBytes;
    }

    public Long getStorageUsedBytes() {
        return storageUsedBytes;
    }

    public void setStorageUsedBytes(Long storageUsedBytes) {
        this.storageUsedBytes = storageUsedBytes;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
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

