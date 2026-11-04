package com.cloudbackup.controller;

import com.cloudbackup.dto.backup.BackupDetailResponse;
import com.cloudbackup.dto.backup.BackupSummaryResponse;
import com.cloudbackup.dto.backup.CreateBackupRequest;
import com.cloudbackup.dto.common.ApiResponse;
import com.cloudbackup.entity.BackupStatus;
import com.cloudbackup.security.UserPrincipal;
import com.cloudbackup.service.BackupService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/backups")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BackupDetailResponse>> createBackup(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateBackupRequest request) {

        BackupDetailResponse response = backupService.createBackup(currentUser.getId(), request, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Backup created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BackupSummaryResponse>>> listBackups(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(value = "status", required = false) BackupStatus status,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<BackupSummaryResponse> backups = backupService.listBackups(currentUser.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Backups retrieved successfully", backups));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<BackupSummaryResponse>>> getBackupHistory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(value = "status", required = false) BackupStatus status,
            @PageableDefault(size = 50, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<BackupSummaryResponse> backups = backupService.listBackups(currentUser.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Backup history retrieved successfully", backups));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BackupDetailResponse>> getBackupById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") UUID id) {

        BackupDetailResponse backup = backupService.getBackupById(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Backup details retrieved", backup));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBackup(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") UUID id) {

        backupService.deleteBackup(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Backup deleted successfully", null));
    }
}
