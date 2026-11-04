package com.cloudbackup.dto.notification;

import com.cloudbackup.entity.Notification;
import com.cloudbackup.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public class NotificationResponse {

    private UUID id;
    private String title;
    private String message;
    private NotificationType notificationType;
    private Boolean isRead;
    private String metadataJson;
    private Instant createdAt;

    public NotificationResponse() {
    }

    public static NotificationResponse fromEntity(Notification entity) {
        NotificationResponse dto = new NotificationResponse();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setMessage(entity.getMessage());
        dto.setNotificationType(entity.getNotificationType());
        dto.setIsRead(entity.getIsRead());
        dto.setMetadataJson(entity.getMetadataJson());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean read) {
        isRead = read;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
