package com.cloudbackup.service;

import com.cloudbackup.dto.analytics.ActivityPointDto;
import com.cloudbackup.dto.analytics.DashboardStatsResponse;
import com.cloudbackup.entity.Backup;
import com.cloudbackup.entity.BackupStatus;
import com.cloudbackup.entity.RecoveryLog;
import com.cloudbackup.entity.User;
import com.cloudbackup.exception.ResourceNotFoundException;
import com.cloudbackup.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final UserRepository userRepository;
    private final FileMetadataRepository fileRepository;
    private final BackupRepository backupRepository;
    private final RecoveryLogRepository recoveryLogRepository;
    private final BackupScheduleRepository scheduleRepository;
    private final NotificationRepository notificationRepository;

    public AnalyticsService(UserRepository userRepository,
                            FileMetadataRepository fileRepository,
                            BackupRepository backupRepository,
                            RecoveryLogRepository recoveryLogRepository,
                            BackupScheduleRepository scheduleRepository,
                            NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
        this.backupRepository = backupRepository;
        this.recoveryLogRepository = recoveryLogRepository;
        this.scheduleRepository = scheduleRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        long totalFiles = fileRepository.countByUserIdAndIsDeletedFalse(userId);
        Long totalStorage = fileRepository.sumSizeBytesByUserId(userId);
        long storageConsumed = totalStorage != null ? totalStorage : 0L;
        long storageQuota = user.getStorageQuotaBytes() != null ? user.getStorageQuotaBytes() : 10737418240L;

        double usedPercentage = 0.0;
        if (storageQuota > 0) {
            usedPercentage = Math.round(((double) storageConsumed / storageQuota) * 10000.0) / 100.0;
        }

        long successfulBackups = backupRepository.countByUserIdAndStatus(userId, BackupStatus.COMPLETED);
        long failedBackups = backupRepository.countByUserIdAndStatus(userId, BackupStatus.FAILED);
        long totalRecoveries = recoveryLogRepository.countByUserId(userId);
        long activeSchedules = scheduleRepository.countByUserIdAndIsActiveTrue(userId);
        long unreadNotifications = notificationRepository.countByUserIdAndIsReadFalse(userId);

        DashboardStatsResponse stats = new DashboardStatsResponse();
        stats.setTotalFilesBackedUp(totalFiles);
        stats.setTotalStorageConsumedBytes(storageConsumed);
        stats.setStorageQuotaBytes(storageQuota);
        stats.setStorageUsedPercentage(usedPercentage);
        stats.setSuccessfulBackups(successfulBackups);
        stats.setFailedBackups(failedBackups);
        stats.setTotalRecoveryOperations(totalRecoveries);
        stats.setActiveSchedules(activeSchedules);
        stats.setUnreadNotifications(unreadNotifications);

        return stats;
    }

    @Transactional(readOnly = true)
    public List<ActivityPointDto> getActivityTrends(UUID userId, int days) {
        int boundedDays = Math.max(1, Math.min(days, 90));
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = endDate.minusDays(boundedDays - 1L);

        List<Backup> backups = backupRepository.findByUserIdAndDateRange(
                userId,
                startDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
                endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        );

        List<RecoveryLog> recoveries = recoveryLogRepository.findByUserIdAndRecoveredAtBetween(
                userId,
                startDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
                endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<ActivityPointDto> points = new ArrayList<>(boundedDays);

        for (int i = 0; i < boundedDays; i++) {
            LocalDate current = startDate.plusDays(i);
            String dateStr = current.format(formatter);

            int backupCount = 0;
            long bytesBackedUp = 0L;
            for (Backup b : backups) {
                if (b.getStartedAt() != null) {
                    LocalDate backupDate = LocalDate.ofInstant(b.getStartedAt(), ZoneOffset.UTC);
                    if (backupDate.equals(current)) {
                        backupCount++;
                        bytesBackedUp += (b.getTotalSizeBytes() != null ? b.getTotalSizeBytes() : 0L);
                    }
                }
            }

            int recoveryCount = 0;
            for (RecoveryLog r : recoveries) {
                if (r.getRecoveredAt() != null) {
                    LocalDate recoveryDate = LocalDate.ofInstant(r.getRecoveredAt(), ZoneOffset.UTC);
                    if (recoveryDate.equals(current)) {
                        recoveryCount++;
                    }
                }
            }

            points.add(new ActivityPointDto(dateStr, backupCount, bytesBackedUp, recoveryCount));
        }

        return points;
    }
}

