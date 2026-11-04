package com.cloudbackup.service;

import com.cloudbackup.dto.analytics.ActivityPointDto;
import com.cloudbackup.dto.analytics.DashboardStatsResponse;
import com.cloudbackup.entity.*;
import com.cloudbackup.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileMetadataRepository fileRepository;

    @Mock
    private BackupRepository backupRepository;

    @Mock
    private RecoveryLogRepository recoveryLogRepository;

    @Mock
    private BackupScheduleRepository scheduleRepository;

    @Mock
    private NotificationRepository notificationRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(
                userRepository,
                fileRepository,
                backupRepository,
                recoveryLogRepository,
                scheduleRepository,
                notificationRepository
        );
    }

    @Test
    @DisplayName("Get dashboard stats computes storage percentage and KPI metrics correctly")
    void testGetDashboardStats() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setStorageQuotaBytes(1000000L); // 1,000,000 bytes

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileRepository.countByUserIdAndIsDeletedFalse(userId)).thenReturn(42L);
        when(fileRepository.sumSizeBytesByUserId(userId)).thenReturn(250000L); // 25% used
        when(backupRepository.countByUserIdAndStatus(userId, BackupStatus.COMPLETED)).thenReturn(10L);
        when(backupRepository.countByUserIdAndStatus(userId, BackupStatus.FAILED)).thenReturn(1L);
        when(recoveryLogRepository.countByUserId(userId)).thenReturn(5L);
        when(scheduleRepository.countByUserIdAndIsActiveTrue(userId)).thenReturn(2L);
        when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(3L);

        DashboardStatsResponse stats = analyticsService.getDashboardStats(userId);

        assertNotNull(stats);
        assertEquals(42L, stats.getTotalFilesBackedUp());
        assertEquals(250000L, stats.getTotalStorageConsumedBytes());
        assertEquals(1000000L, stats.getStorageQuotaBytes());
        assertEquals(25.0, stats.getStorageUsedPercentage());
        assertEquals(10L, stats.getSuccessfulBackups());
        assertEquals(1L, stats.getFailedBackups());
        assertEquals(5L, stats.getTotalRecoveryOperations());
        assertEquals(2L, stats.getActiveSchedules());
        assertEquals(3L, stats.getUnreadNotifications());
    }

    @Test
    @DisplayName("Get activity trends returns correct daily buckets")
    void testGetActivityTrends() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        Backup backupToday = new Backup(user, "Today's Backup", "desc");
        backupToday.setStartedAt(Instant.now());
        backupToday.addVersion(new BackupVersion(backupToday, new FileMetadata(), 1, "k1", "h1", 1024L));

        when(backupRepository.findByUserIdAndDateRange(eq(userId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(backupToday));

        when(recoveryLogRepository.findByUserIdAndRecoveredAtBetween(eq(userId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        List<ActivityPointDto> trends = analyticsService.getActivityTrends(userId, 7);

        assertNotNull(trends);
        assertEquals(7, trends.size(), "Should have exactly 7 daily data points");

        // The last point corresponds to today
        ActivityPointDto todayPoint = trends.get(6);
        assertEquals(1, todayPoint.getBackupCount());
        assertTrue(todayPoint.getBytesBackedUp() > 0);
        assertEquals(0, todayPoint.getRecoveryCount());
    }
}

