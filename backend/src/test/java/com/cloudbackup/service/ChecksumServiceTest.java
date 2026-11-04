package com.cloudbackup.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ChecksumServiceTest {

    private ChecksumService checksumService;

    @BeforeEach
    void setUp() {
        checksumService = new ChecksumService();
    }

    @Test
    @DisplayName("Calculate SHA-256 for known string produces deterministic hash")
    void testCalculateSha256KnownString() {
        byte[] input = "CloudBackupEnterprise2026".getBytes(StandardCharsets.UTF_8);
        String hash = checksumService.calculateSha256(input);

        assertNotNull(hash);
        assertEquals(64, hash.length(), "SHA-256 hash must be 64 hexadecimal characters");

        // Running twice on identical bytes produces identical hash
        String hash2 = checksumService.calculateSha256(input);
        assertEquals(hash, hash2);
    }

    @Test
    @DisplayName("Calculate SHA-256 from InputStream matches byte array calculation")
    void testCalculateSha256InputStream() {
        byte[] input = "Deterministic backup payload verification sample".getBytes(StandardCharsets.UTF_8);
        String byteHash = checksumService.calculateSha256(input);

        String streamHash = checksumService.calculateSha256(new ByteArrayInputStream(input));
        assertEquals(byteHash, streamHash);
    }

    @Test
    @DisplayName("Verify checksum passes for valid match and fails for corrupted input")
    void testVerifyChecksum() {
        byte[] original = "Original uncorrupted document".getBytes(StandardCharsets.UTF_8);
        byte[] corrupted = "Tampered corrupted document".getBytes(StandardCharsets.UTF_8);

        String originalHash = checksumService.calculateSha256(original);

        assertTrue(checksumService.verifyChecksum(new ByteArrayInputStream(original), originalHash));
        assertFalse(checksumService.verifyChecksum(new ByteArrayInputStream(corrupted), originalHash));
    }
}

