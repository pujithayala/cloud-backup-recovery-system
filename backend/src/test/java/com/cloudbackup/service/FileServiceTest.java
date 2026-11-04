package com.cloudbackup.service;

import com.cloudbackup.dto.file.FileMetadataResponse;
import com.cloudbackup.dto.file.FileUploadResponse;
import com.cloudbackup.entity.FileMetadata;
import com.cloudbackup.entity.User;
import com.cloudbackup.exception.BadRequestException;
import com.cloudbackup.exception.QuotaExceededException;
import com.cloudbackup.repository.FileMetadataRepository;
import com.cloudbackup.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileMetadataRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService(fileRepository, userRepository);
    }

    @Test
    @DisplayName("Upload file succeeds and updates user storage consumption")
    void testUploadFileSuccess() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setStorageQuotaBytes(1048576L); // 1 MB quota
        user.setStorageUsedBytes(0L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "finance-report.pdf",
                "application/pdf",
                "Report content data".getBytes(StandardCharsets.UTF_8)
        );

        when(fileRepository.save(any(FileMetadata.class))).thenAnswer(invocation -> {
            FileMetadata saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        FileUploadResponse response = fileService.uploadFiles(userId, List.of(file), "/finance");

        assertNotNull(response);
        assertEquals(1, response.getUploadedCount());
        assertTrue(response.getTotalSizeBytes() > 0);
        assertTrue(user.getStorageUsedBytes() > 0);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Upload file fails when exceeding user storage quota")
    void testUploadFileQuotaExceeded() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setStorageQuotaBytes(100L); // 100 bytes quota
        user.setStorageUsedBytes(90L);   // 10 bytes available

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MockMultipartFile bigFile = new MockMultipartFile(
                "files",
                "large-archive.zip",
                "application/zip",
                new byte[500] // 500 bytes exceeds remaining 10 bytes
        );

        assertThrows(QuotaExceededException.class, () ->
                fileService.uploadFiles(userId, List.of(bigFile), "/backups")
        );
        verify(fileRepository, never()).save(any(FileMetadata.class));
    }

    @Test
    @DisplayName("Upload file rejects path traversal attempts in filename")
    void testUploadFileDirectoryTraversalCheck() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setStorageQuotaBytes(100000L);
        user.setStorageUsedBytes(0L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MockMultipartFile maliciousFile = new MockMultipartFile(
                "files",
                "../../etc/passwd",
                "text/plain",
                "malicious data".getBytes()
        );

        assertThrows(BadRequestException.class, () ->
                fileService.uploadFiles(userId, List.of(maliciousFile), "/")
        );
    }

    @Test
    @DisplayName("Soft delete file marks isDeleted flag and deducts storage usage")
    void testSoftDeleteFile() {
        UUID userId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setStorageUsedBytes(5000L);

        FileMetadata file = new FileMetadata(user, "notes.txt", "/docs", "text/plain", 1000L);
        file.setId(fileId);
        file.setIsDeleted(false);

        when(fileRepository.findByIdAndUserIdAndIsDeletedFalse(fileId, userId)).thenReturn(Optional.of(file));

        fileService.softDeleteFile(fileId, userId);

        assertTrue(file.getIsDeleted());
        assertEquals(4000L, user.getStorageUsedBytes());
        verify(fileRepository, times(1)).save(file);
        verify(userRepository, times(1)).save(user);
    }
}

