package com.cloudbackup.service;

import com.cloudbackup.exception.StorageException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@Primary
public class MinioStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;
    private final LocalStorageService localStorageService;
    private final String bucketName;
    private final boolean preferLocal;
    private volatile boolean minioAvailable = false;

    public MinioStorageService(MinioClient minioClient,
                               LocalStorageService localStorageService,
                               @Value("${app.storage.bucket-name:cloud-backups}") String bucketName,
                               @Value("${app.storage.type:s3}") String storageType) {
        this.minioClient = minioClient;
        this.localStorageService = localStorageService;
        this.bucketName = bucketName;
        this.preferLocal = "local".equalsIgnoreCase(storageType);
    }

    @PostConstruct
    public void init() {
        if (preferLocal) {
            log.info("Storage configured to use local filesystem storage.");
            minioAvailable = false;
            return;
        }

        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Initialized default cloud storage bucket: {}", bucketName);
            } else {
                log.info("Storage bucket '{}' verified.", bucketName);
            }
            minioAvailable = true;
        } catch (Exception e) {
            log.warn("MinIO bucket check failed (storage daemon might not be running). Falling back to local disk storage: {}", e.getMessage());
            minioAvailable = false;
        }
    }

    @Override
    public void upload(String storageKey, InputStream inputStream, long size, String contentType) {
        if (!minioAvailable && !preferLocal) {
            // Check if MinIO has become available
            checkMinioHealth();
        }

        if (minioAvailable && !preferLocal) {
            try {
                ensureBucketExists();
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(storageKey)
                                .stream(inputStream, size, -1)
                                .contentType(contentType != null ? contentType : "application/octet-stream")
                                .build()
                );
                log.debug("Uploaded object {} (size: {} bytes) to MinIO bucket {}", storageKey, size, bucketName);
                return;
            } catch (Exception e) {
                log.warn("Failed upload to MinIO (falling back to local storage): {}", e.getMessage());
                minioAvailable = false;
            }
        }

        // Fallback to local disk storage
        localStorageService.upload(storageKey, inputStream, size, contentType);
    }

    @Override
    public InputStream download(String storageKey) {
        if (minioAvailable && !preferLocal) {
            try {
                return minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(storageKey)
                                .build()
                );
            } catch (Exception e) {
                log.warn("Failed download from MinIO for key {} (checking local fallback): {}", storageKey, e.getMessage());
            }
        }

        // Fallback to local disk storage
        return localStorageService.download(storageKey);
    }

    @Override
    public void delete(String storageKey) {
        if (minioAvailable && !preferLocal) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(storageKey)
                                .build()
                );
                log.debug("Deleted object {} from MinIO bucket {}", storageKey, bucketName);
            } catch (Exception e) {
                log.warn("Failed delete from MinIO: {}", e.getMessage());
            }
        }

        if (localStorageService.exists(storageKey)) {
            localStorageService.delete(storageKey);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        if (minioAvailable && !preferLocal) {
            try {
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(storageKey)
                                .build()
                );
                return true;
            } catch (ErrorResponseException e) {
                if ("NoSuchKey".equals(e.errorResponse().code())) {
                    return localStorageService.exists(storageKey);
                }
            } catch (Exception e) {
                log.debug("MinIO stat error: {}", e.getMessage());
            }
        }

        return localStorageService.exists(storageKey);
    }

    @Override
    public long getObjectSize(String storageKey) {
        if (minioAvailable && !preferLocal) {
            try {
                StatObjectResponse stat = minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(storageKey)
                                .build()
                );
                return stat.size();
            } catch (Exception e) {
                log.debug("MinIO getObjectSize failed: {}", e.getMessage());
            }
        }

        return localStorageService.getObjectSize(storageKey);
    }

    private void checkMinioHealth() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            minioAvailable = true;
            log.info("MinIO service connection restored.");
        } catch (Exception e) {
            minioAvailable = false;
        }
    }

    private void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created missing storage bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to ensure bucket exists: {}", e.getMessage());
        }
    }
}
