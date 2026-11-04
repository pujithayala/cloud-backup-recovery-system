package com.cloudbackup.controller;

import com.cloudbackup.dto.common.ApiResponse;
import com.cloudbackup.dto.recovery.BatchRecoveryRequest;
import com.cloudbackup.dto.recovery.IntegrityCheckResponse;
import com.cloudbackup.dto.recovery.RecoveryLogResponse;
import com.cloudbackup.security.UserPrincipal;
import com.cloudbackup.service.RecoveryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recovery")
public class RecoveryController {

    private final RecoveryService recoveryService;

    public RecoveryController(RecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @PostMapping("/restore-version/{versionId}")
    public ResponseEntity<ApiResponse<RecoveryLogResponse>> restoreVersion(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("versionId") UUID versionId,
            @RequestParam(value = "destinationPath", required = false, defaultValue = "/recovered") String destinationPath) {

        RecoveryLogResponse response = recoveryService.restoreVersion(versionId, currentUser.getId(), destinationPath);
        return ResponseEntity.ok(ApiResponse.success("Version restored successfully", response));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<RecoveryLogResponse>>> batchRestore(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody BatchRecoveryRequest request) {

        List<RecoveryLogResponse> responses = recoveryService.batchRestore(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Batch recovery completed", responses));
    }

    @PostMapping("/verify-integrity/{versionId}")
    public ResponseEntity<ApiResponse<IntegrityCheckResponse>> verifyIntegrity(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("versionId") UUID versionId) {

        IntegrityCheckResponse response = recoveryService.verifyIntegrity(versionId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("File integrity check completed", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<RecoveryLogResponse>>> getRecoveryHistory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PageableDefault(size = 20, sort = "recoveredAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<RecoveryLogResponse> history = recoveryService.getRecoveryHistory(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Recovery history retrieved", history));
    }
}
