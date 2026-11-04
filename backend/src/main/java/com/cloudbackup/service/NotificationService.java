package com.cloudbackup.service;

import com.cloudbackup.dto.notification.NotificationResponse;
import com.cloudbackup.entity.Notification;
import com.cloudbackup.entity.NotificationType;
import com.cloudbackup.entity.User;
import com.cloudbackup.exception.ResourceNotFoundException;
import com.cloudbackup.repository.NotificationRepository;
import com.cloudbackup.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createNotification(UUID userId, String title, String message, NotificationType type, String metadata) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Cannot dispatch notification to nonexistent user: {}", userId);
            return;
        }

        Notification notification = new Notification(user, title, message, type, metadata);
        notificationRepository.save(notification);
        log.info("Dispatched notification '{}' of type {} to user {}", title, type, userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotificationsPaged(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }
}
