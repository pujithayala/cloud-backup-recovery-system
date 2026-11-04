package com.cloudbackup.service;

import com.cloudbackup.dto.schedule.ScheduleRequest;
import com.cloudbackup.dto.schedule.ScheduleResponse;
import com.cloudbackup.entity.BackupSchedule;
import com.cloudbackup.entity.NotificationType;
import com.cloudbackup.entity.ScheduleFrequency;
import com.cloudbackup.entity.User;
import com.cloudbackup.exception.BadRequestException;
import com.cloudbackup.repository.BackupScheduleRepository;
import com.cloudbackup.repository.FileMetadataRepository;
import com.cloudbackup.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private BackupScheduleRepository scheduleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileMetadataRepository fileRepository;

    @Mock
    private BackupService backupService;

    @Mock
    private NotificationService notificationService;

    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(
                scheduleRepository,
                userRepository,
                fileRepository,
                backupService,
                notificationService
        );
    }

    @Test
    @DisplayName("Create daily schedule sets proper cron and calculates next run")
    void testCreateDailySchedule() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scheduleRepository.save(any(BackupSchedule.class))).thenAnswer(invocation -> {
            BackupSchedule s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        ScheduleRequest request = new ScheduleRequest();
        request.setScheduleName("Nightly Database Backup");
        request.setFrequency(ScheduleFrequency.DAILY);
        request.setTargetDirectory("/data");

        ScheduleResponse response = scheduleService.createSchedule(userId, request);

        assertNotNull(response);
        assertEquals("Nightly Database Backup", response.getScheduleName());
        assertEquals(ScheduleFrequency.DAILY, response.getFrequency());
        assertEquals("0 0 2 * * ?", response.getCronExpression());
        assertTrue(response.getIsActive());
        assertNotNull(response.getNextRunAt());

        verify(notificationService, times(1)).createNotification(
                eq(userId), contains("Created"), anyString(), eq(NotificationType.SCHEDULE_TRIGGERED), anyString()
        );
    }

    @Test
    @DisplayName("Create custom schedule rejects invalid cron format")
    void testCreateCustomScheduleInvalidCron() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ScheduleRequest request = new ScheduleRequest();
        request.setScheduleName("Bad Cron Schedule");
        request.setFrequency(ScheduleFrequency.CUSTOM);
        request.setCronExpression("invalid * cron * format * here *");

        assertThrows(BadRequestException.class, () -> scheduleService.createSchedule(userId, request));
    }

    @Test
    @DisplayName("Toggle schedule switches isActive state and updates nextRunAt")
    void testToggleSchedule() {
        UUID userId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        BackupSchedule schedule = new BackupSchedule();
        schedule.setId(scheduleId);
        schedule.setScheduleName("Weekly Backup");
        schedule.setFrequency(ScheduleFrequency.WEEKLY);
        schedule.setCronExpression("0 0 2 ? * SUN");
        schedule.setIsActive(true);

        when(scheduleRepository.findByIdAndUserId(scheduleId, userId)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(BackupSchedule.class))).thenAnswer(i -> i.getArgument(0));

        // Toggle from active to inactive
        ScheduleResponse response1 = scheduleService.toggleSchedule(scheduleId, userId);
        assertFalse(response1.getIsActive());
        assertNull(response1.getNextRunAt());

        // Toggle from inactive to active
        ScheduleResponse response2 = scheduleService.toggleSchedule(scheduleId, userId);
        assertTrue(response2.getIsActive());
        assertNotNull(response2.getNextRunAt());
    }
}

