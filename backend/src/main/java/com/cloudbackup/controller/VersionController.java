package com.cloudbackup.controller;

import com.cloudbackup.dto.backup.BackupVersionResponse;
import com.cloudbackup.dto.common.ApiResponse;
import com.cloudbackup.entity.BackupVersion;
import com.cloudbackup.exception.ResourceNotFoundException;
import com.cloudbackup.repository.BackupVersionRepository;
import com.cloudbackup.security.UserPrincipal;
import com.cloudbackup.service.BackupService;
import com.cloudbackup.service.StorageService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/versions")
public class VersionController {

    private final BackupService backupService;
    private final BackupVersionRepository versionRepository;
    private final StorageService storageService;

    public VersionController(BackupService backupService,
                             BackupVersionRepository versionRepository,
                             StorageService storageService) {
        this.backupService = backupService;
        this.versionRepository = versionRepository;
        this.storageService = storageService;
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<List<BackupVersionResponse>>> getFileVersions(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("fileId") UUID fileId) {

        List<BackupVersionResponse> versions = backupService.getVersionsForFile(fileId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Versions retrieved successfully", versions));
    }

    @GetMapping("/{versionId}/download")
    public ResponseEntity<InputStreamResource> downloadVersion(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("versionId") UUID versionId) {

        BackupVersion version = versionRepository.findByIdAndUserId(versionId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("BackupVersion", "id", versionId));

        InputStream stream = storageService.download(version.getStorageKey());
        String filename = version.getFile().getFilename();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, version.getFile().getContentType())
                .body(new InputStreamResource(stream));
    }
}
