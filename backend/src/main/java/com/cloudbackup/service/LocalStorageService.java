package com.cloudbackup.service;

import com.cloudbackup.exception.StorageException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service("localStorageService")
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path rootPath;

    public LocalStorageService(@Value("${app.storage.local-directory:./data/cloud-storage}") String storageDir) {
        this.rootPath = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootPath);
            log.info("Initialized local disk storage at: {}", this.rootPath);
        } catch (Exception e) {
            log.error("Failed to initialize local disk storage directory: {}", e.getMessage());
        }
    }

    private Path resolveKey(String storageKey) {
        Path resolved = rootPath.resolve(storageKey).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new StorageException("Security error: directory traversal attempt for key " + storageKey);
        }
        return resolved;
    }

    @Override
    public void upload(String storageKey, InputStream inputStream, long size, String contentType) {
        try {
            Path targetFile = resolveKey(storageKey);
            Files.createDirectories(targetFile.getParent());
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored local object: {} (size: {} bytes)", storageKey, Files.size(targetFile));
        } catch (Exception e) {
            throw new StorageException("Failed to save object to local storage: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String storageKey) {
        try {
            Path targetFile = resolveKey(storageKey);
            if (!Files.exists(targetFile)) {
                throw new StorageException("Object not found in local storage: " + storageKey);
            }
            return new FileInputStream(targetFile.toFile());
        } catch (Exception e) {
            throw new StorageException("Failed to read object from local storage: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path targetFile = resolveKey(storageKey);
            if (Files.exists(targetFile)) {
                Files.delete(targetFile);
                log.debug("Deleted local object: {}", storageKey);
            }
        } catch (Exception e) {
            throw new StorageException("Failed to delete object from local storage: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            Path targetFile = resolveKey(storageKey);
            return Files.exists(targetFile);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long getObjectSize(String storageKey) {
        try {
            Path targetFile = resolveKey(storageKey);
            if (!Files.exists(targetFile)) {
                throw new StorageException("Object not found in local storage: " + storageKey);
            }
            return Files.size(targetFile);
        } catch (Exception e) {
            throw new StorageException("Failed to get local object size: " + e.getMessage(), e);
        }
    }
}

