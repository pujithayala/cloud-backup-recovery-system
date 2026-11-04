package com.cloudbackup.service;

import com.cloudbackup.dto.file.FileMetadataResponse;
import com.cloudbackup.dto.file.FileUploadResponse;
import com.cloudbackup.entity.FileMetadata;
import com.cloudbackup.entity.User;
import com.cloudbackup.exception.BadRequestException;
import com.cloudbackup.exception.QuotaExceededException;
import com.cloudbackup.exception.ResourceNotFoundException;
import com.cloudbackup.repository.FileMetadataRepository;
import com.cloudbackup.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileMetadataRepository fileRepository;
    private final UserRepository userRepository;

    public FileService(FileMetadataRepository fileRepository, UserRepository userRepository) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public FileUploadResponse uploadFiles(UUID userId, List<MultipartFile> files, String folderPath) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("At least one file must be selected for upload");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        long totalBatchSize = files.stream().mapToLong(MultipartFile::getSize).sum();
        if (!user.canStoreAdditional(totalBatchSize)) {
            throw new QuotaExceededException(
                    String.format("Upload exceeds quota. Required: %d bytes, Available: %d bytes",
                            totalBatchSize, (user.getStorageQuotaBytes() - user.getStorageUsedBytes()))
            );
        }

        String sanitizedFolder = sanitizeFolderPath(folderPath);
        List<FileMetadataResponse> uploadedResponses = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            String rawFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed_file");
            // Security check against directory traversal
            if (rawFilename.contains("..")) {
                throw new BadRequestException("Invalid filename with path traversal: " + rawFilename);
            }

            String contentType = file.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            FileMetadata fileMetadata = new FileMetadata(
                    user,
                    rawFilename,
                    sanitizedFolder,
                    contentType,
                    file.getSize()
            );

            FileMetadata savedFile = fileRepository.save(fileMetadata);
            uploadedResponses.add(FileMetadataResponse.fromEntity(savedFile));
        }

        user.addStorageUsage(totalBatchSize);
        userRepository.save(user);

        log.info("Uploaded {} files (total {} bytes) for user {}", uploadedResponses.size(), totalBatchSize, userId);
        return new FileUploadResponse(uploadedResponses.size(), totalBatchSize, uploadedResponses);
    }

    @Transactional(readOnly = true)
    public Page<FileMetadataResponse> listFiles(UUID userId, String search, String folder, Pageable pageable) {
        Page<FileMetadata> filePage;

        if (StringUtils.hasText(search)) {
            filePage = fileRepository.findByUserIdAndFilenameContainingIgnoreCaseAndIsDeletedFalse(userId, search.trim(), pageable);
        } else if (StringUtils.hasText(folder)) {
            filePage = fileRepository.findByUserIdAndOriginalPathAndIsDeletedFalse(userId, sanitizeFolderPath(folder), pageable);
        } else {
            filePage = fileRepository.findByUserIdAndIsDeletedFalse(userId, pageable);
        }

        return filePage.map(FileMetadataResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public FileMetadataResponse getFileById(UUID fileId, UUID userId) {
        FileMetadata file = fileRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));
        return FileMetadataResponse.fromEntity(file);
    }

    @Transactional
    public void softDeleteFile(UUID fileId, UUID userId) {
        FileMetadata file = fileRepository.findByIdAndUserIdAndIsDeletedFalse(fileId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Active File", "id", fileId));

        file.setIsDeleted(true);
        fileRepository.save(file);

        User user = file.getUser();
        user.reduceStorageUsage(file.getSizeBytes());
        userRepository.save(user);

        log.info("Soft-deleted file {} for user {}", fileId, userId);
    }

    @Transactional
    public FileMetadataResponse restoreSoftDeletedFile(UUID fileId, UUID userId) {
        FileMetadata file = fileRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        if (!file.getIsDeleted()) {
            return FileMetadataResponse.fromEntity(file);
        }

        User user = file.getUser();
        if (!user.canStoreAdditional(file.getSizeBytes())) {
            throw new QuotaExceededException("Cannot restore file: storage quota would be exceeded");
        }

        file.setIsDeleted(false);
        fileRepository.save(file);

        user.addStorageUsage(file.getSizeBytes());
        userRepository.save(user);

        log.info("Restored soft-deleted file {} for user {}", fileId, userId);
        return FileMetadataResponse.fromEntity(file);
    }

    private String sanitizeFolderPath(String folderPath) {
        if (!StringUtils.hasText(folderPath) || "/".equals(folderPath.trim())) {
            return "/";
        }
        String clean = folderPath.trim().replace('\\', '/');
        if (!clean.startsWith("/")) {
            clean = "/" + clean;
        }
        if (clean.endsWith("/") && clean.length() > 1) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }
}
