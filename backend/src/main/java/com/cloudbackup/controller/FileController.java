package com.cloudbackup.controller;

import com.cloudbackup.dto.common.ApiResponse;
import com.cloudbackup.dto.file.FileMetadataResponse;
import com.cloudbackup.dto.file.FileUploadResponse;
import com.cloudbackup.security.UserPrincipal;
import com.cloudbackup.service.FileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFiles(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folderPath", required = false, defaultValue = "/") String folderPath) {

        FileUploadResponse response = fileService.uploadFiles(currentUser.getId(), files, folderPath);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Files uploaded successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FileMetadataResponse>>> listFiles(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "folder", required = false) String folder,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<FileMetadataResponse> files = fileService.listFiles(currentUser.getId(), search, folder, pageable);
        return ResponseEntity.ok(ApiResponse.success("Files retrieved successfully", files));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FileMetadataResponse>> getFileById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") UUID id) {

        FileMetadataResponse file = fileService.getFileById(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("File retrieved successfully", file));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") UUID id) {

        fileService.softDeleteFile(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<FileMetadataResponse>> restoreFile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") UUID id) {

        FileMetadataResponse restored = fileService.restoreSoftDeletedFile(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("File restored successfully", restored));
    }
}
