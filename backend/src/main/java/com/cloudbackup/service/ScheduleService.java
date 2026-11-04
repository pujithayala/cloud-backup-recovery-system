package com.cloudbackup.service;

import com.cloudbackup.dto.backup.BackupDetailResponse;
import com.cloudbackup.dto.backup.CreateBackupRequest;
import com.cloudbackup.dto.schedule.ScheduleRequest;
import com.cloudbackup.dto.schedule.ScheduleResponse;
import com.cloudbackup.entity.*;
import com.cloudbackup.exception.BadRequestException;
import com.cloudbackup.exception.ResourceNotFoundException;
import com.cloudbackup.repository.BackupScheduleRepository;
import com.cloudbackup.repository.FileMetadataRepository;
import com.cloudbackup.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

    private final BackupScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final FileMetadataRepository fileRepository;
    private final BackupService backupService;
    private final NotificationService notificationService;

    public ScheduleService(BackupScheduleRepository scheduleRepository,
                           UserRepository userRepository,
                           FileMetadataRepository fileRepository,
                           BackupService backupService,
                           NotificationService notificationService) {
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
        this.backupService = backupService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ScheduleResponse createSchedule(UUID userId, ScheduleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String resolvedCron = resolveCronExpression(request.getFrequency(), request.getCronExpression());

        BackupSchedule schedule = new BackupSchedule();
        schedule.setUser(user);
        schedule.setScheduleName(request.getScheduleName().trim());
        schedule.setCronExpression(resolvedCron);
        schedule.setFrequency(request.getFrequency());
        schedule.setTargetDirectory(request.getTargetDirectory());
        schedule.setIsActive(true);
        schedule.setNextRunAt(calculateNextRun(resolvedCron));

        BackupSchedule saved = scheduleRepository.save(schedule);
        log.info("Created schedule '{}' (id: {}) for user {}", saved.getScheduleName(), saved.getId(), userId);

        notificationService.createNotification(
                userId,
                "Backup Schedule Created",
                String.format("Automatic backup schedule '%s' has been configured (%s).", saved.getScheduleName(), saved.getFrequency()),
                NotificationType.SCHEDULE_TRIGGERED,
                "{\"scheduleId\":\"" + saved.getId() + "\"}"
        );

        return ScheduleResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> listUserSchedules(UUID userId) {
        return scheduleRepository.findByUserId(userId).stream()
                .map(ScheduleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ScheduleResponse toggleSchedule(UUID scheduleId, UUID userId) {
        BackupSchedule schedule = scheduleRepository.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));

        boolean newState = !schedule.getIsActive();
        schedule.setIsActive(newState);

        if (newState) {
            schedule.setNextRunAt(calculateNextRun(schedule.getCronExpression()));
        } else {
            schedule.setNextRunAt(null);
        }

        BackupSchedule saved = scheduleRepository.save(schedule);
        log.info("Schedule {} active state set to {}", scheduleId, newState);
        return ScheduleResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteSchedule(UUID scheduleId, UUID userId) {
        BackupSchedule schedule = scheduleRepository.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));
        scheduleRepository.delete(schedule);
        log.info("Deleted schedule {} for user {}", scheduleId, userId);
    }

    @Transactional
    public BackupDetailResponse triggerNow(UUID scheduleId, UUID userId) {
        BackupSchedule schedule = scheduleRepository.findByIdAndUserId(scheduleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));

        return executeScheduledBackup(schedule);
    }

    @Transactional
    public BackupDetailResponse executeScheduledBackup(BackupSchedule schedule) {
        UUID userId = schedule.getUser().getId();
        List<FileMetadata> files;

        if ("/".equals(schedule.getTargetDirectory())) {
            files = fileRepository.findByUserIdAndIsDeletedFalse(userId);
        } else {
            files = fileRepository.findByUserIdAndIsDeletedFalse(userId).stream()
                    .filter(f -> f.getOriginalPath().startsWith(schedule.getTargetDirectory()))
                    .collect(Collectors.toList());
        }

        if (files.isEmpty()) {
            log.info("Schedule {}: no files found to backup in directory {}", schedule.getId(), schedule.getTargetDirectory());
            schedule.setLastRunAt(Instant.now());
            schedule.setNextRunAt(calculateNextRun(schedule.getCronExpression()));
            scheduleRepository.save(schedule);
            return null;
        }

        List<UUID> fileIds = files.stream().map(FileMetadata::getId).collect(Collectors.toList());
        CreateBackupRequest request = new CreateBackupRequest(
                schedule.getScheduleName() + " - " + Instant.now().toString().substring(0, 10),
                "Automated scheduled backup triggered by schedule ID: " + schedule.getId(),
                fileIds
        );

        BackupDetailResponse backupDetail = backupService.createBackup(userId, request, schedule);

        schedule.setLastRunAt(Instant.now());
        schedule.setNextRunAt(calculateNextRun(schedule.getCronExpression()));
        scheduleRepository.save(schedule);

        log.info("Executed scheduled backup for schedule {} resulting in backup {}", schedule.getId(), backupDetail.getId());
        return backupDetail;
    }

    private String resolveCronExpression(ScheduleFrequency frequency, String customCron) {
        if (frequency == ScheduleFrequency.CUSTOM) {
            if (customCron == null || customCron.isBlank()) {
                throw new BadRequestException("Custom frequency requires a valid cron expression");
            }
            try {
                CronExpression.parse(customCron);
                return customCron;
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid cron expression format: " + e.getMessage());
            }
        }

        return switch (frequency) {
            case DAILY -> "0 0 2 * * ?";       // Everyday at 2:00 AM
            case WEEKLY -> "0 0 2 ? * SUN";    // Every Sunday at 2:00 AM
            case MONTHLY -> "0 0 2 1 * ?";     // 1st of every month at 2:00 AM
            default -> "0 0 2 * * ?";
        };
    }

    private Instant calculateNextRun(String cronExpression) {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            ZonedDateTime next = cron.next(ZonedDateTime.now(ZoneId.systemDefault()));
            return next != null ? next.toInstant() : null;
        } catch (Exception e) {
            log.warn("Could not calculate next run for cron '{}': {}", cronExpression, e.getMessage());
            return Instant.now().plusSeconds(86400); // 24h fallback
        }
    }
}
