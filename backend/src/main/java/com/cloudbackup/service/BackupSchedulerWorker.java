package com.cloudbackup.service;

import com.cloudbackup.entity.BackupSchedule;
import com.cloudbackup.repository.BackupScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class BackupSchedulerWorker {

    private static final Logger log = LoggerFactory.getLogger(BackupSchedulerWorker.class);

    private final BackupScheduleRepository scheduleRepository;
    private final ScheduleService scheduleService;

    public BackupSchedulerWorker(BackupScheduleRepository scheduleRepository, ScheduleService scheduleService) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleService = scheduleService;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 15000)
    public void scanAndRunScheduledBackups() {
        try {
            List<BackupSchedule> activeSchedules = scheduleRepository.findByIsActiveTrue();
            Instant now = Instant.now();

            for (BackupSchedule schedule : activeSchedules) {
                if (schedule.getNextRunAt() != null && !schedule.getNextRunAt().isAfter(now)) {
                    log.info("Schedule '{}' ({}) is due for execution. Triggering backup...", schedule.getScheduleName(), schedule.getId());
                    try {
                        scheduleService.executeScheduledBackup(schedule);
                    } catch (Exception ex) {
                        log.error("Failed scheduled backup execution for schedule {}: {}", schedule.getId(), ex.getMessage(), ex);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error running scheduler worker: {}", e.getMessage(), e);
        }
    }
}
