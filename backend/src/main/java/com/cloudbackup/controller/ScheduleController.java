package com.cloudbackup.controller;

import com.cloudbackup.dto.backup.BackupDetailResponse;
import com.cloudbackup.dto.common.ApiResponse;
import com.cloudbackup.dto.schedule.ScheduleRequest;
import com.cloudbackup.dto.schedule.ScheduleResponse;
import com.cloudbackup.security.UserPrincipal;
import com.cloudbackup.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody ScheduleRequest request) {

        ScheduleResponse response = scheduleService.createSchedule(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Schedule created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> listSchedules(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        List<ScheduleResponse> schedules = scheduleService.listUserSchedules(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Schedules retrieved successfully", schedules));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<ScheduleResponse>> toggleSchedule(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") UUID id) {

        ScheduleResponse toggled = scheduleService.toggleSchedule(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Schedule state updated", toggled));
    }

    @PostMapping("/{id}/trigger-now")
    public ResponseEntity<ApiResponse<BackupDetailResponse>> triggerNow(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") UUID id) {

        BackupDetailResponse backup = scheduleService.triggerNow(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Schedule triggered successfully", backup));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") UUID id) {

        scheduleService.deleteSchedule(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Schedule deleted successfully", null));
    }
}
