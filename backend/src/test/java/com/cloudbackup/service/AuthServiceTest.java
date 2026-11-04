package com.cloudbackup.service;

import com.cloudbackup.dto.auth.AuthResponse;
import com.cloudbackup.dto.auth.LoginRequest;
import com.cloudbackup.dto.auth.RegisterRequest;
import com.cloudbackup.dto.auth.UserSummaryDto;
import com.cloudbackup.entity.Role;
import com.cloudbackup.entity.User;
import com.cloudbackup.exception.BadRequestException;
import com.cloudbackup.exception.ResourceNotFoundException;
import com.cloudbackup.repository.UserRepository;
import com.cloudbackup.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    private final Long defaultQuota = 10737418240L; // 10 GB

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtTokenProvider, defaultQuota);
    }

    @Test
    @DisplayName("Register user successfully with hashed password and initial quota")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setFullName("John Doe");
        request.setPassword("SecureP@ss123");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed_pass");

        UUID generatedId = UUID.randomUUID();
        User savedUser = new User();
        savedUser.setId(generatedId);
        savedUser.setEmail("user@example.com");
        savedUser.setFullName("John Doe");
        savedUser.setRole(Role.ROLE_USER);
        savedUser.setStorageQuotaBytes(defaultQuota);
        savedUser.setStorageUsedBytes(0L);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateTokenForUser(eq(generatedId), eq("user@example.com"), eq("John Doe")))
                .thenReturn("mock-jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("user@example.com", response.getUser().getEmail());
        assertEquals("John Doe", response.getUser().getFullName());
        assertEquals(defaultQuota, response.getUser().getStorageQuotaBytes());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Register user throws BadRequestException if email already registered")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setFullName("Existing User");
        request.setPassword("Password123!");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Login user generates JWT token on valid credentials")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@cloud.io");
        request.setPassword("Secret123!");

        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mockAuth);
        when(jwtTokenProvider.generateToken(mockAuth)).thenReturn("auth-token-jwt");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@cloud.io");
        user.setFullName("Admin User");
        user.setRole(Role.ROLE_ADMIN);
        when(userRepository.findByEmail("admin@cloud.io")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("auth-token-jwt", response.getToken());
        assertEquals("admin@cloud.io", response.getUser().getEmail());
    }

    @Test
    @DisplayName("Get current user profile returns UserSummaryDto")
    void testGetCurrentUserProfile() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("profile@cloud.io");
        user.setFullName("Profile User");
        user.setRole(Role.ROLE_USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserSummaryDto profile = authService.getCurrentUserProfile(userId);

        assertNotNull(profile);
        assertEquals("profile@cloud.io", profile.getEmail());
        assertEquals("Profile User", profile.getFullName());
    }

    @Test
    @DisplayName("Get profile throws ResourceNotFoundException when user does not exist")
    void testGetProfileNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.getCurrentUserProfile(userId));
    }
}

