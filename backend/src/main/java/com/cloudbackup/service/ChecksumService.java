package com.cloudbackup.service;

import com.cloudbackup.exception.AppException;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class ChecksumService {

    private static final String ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 8192;

    public String calculateSha256(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            BufferedInputStream bis = (inputStream instanceof BufferedInputStream)
                    ? (BufferedInputStream) inputStream
                    : new BufferedInputStream(inputStream);

            while ((bytesRead = bis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] hashBytes = digest.digest();
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new AppException("Failed to compute SHA-256 checksum: " + e.getMessage(), e);
        }
    }

    public String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(data);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new AppException("SHA-256 algorithm not available", e);
        }
    }

    public boolean verifyChecksum(InputStream inputStream, String expectedChecksum) {
        if (expectedChecksum == null || expectedChecksum.isBlank()) {
            return false;
        }
        String calculated = calculateSha256(inputStream);
        return calculated.equalsIgnoreCase(expectedChecksum.trim());
    }
}

