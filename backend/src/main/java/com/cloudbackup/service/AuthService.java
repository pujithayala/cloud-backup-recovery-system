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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final Long defaultQuotaBytes;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.user.default-quota-bytes:10737418240}") Long defaultQuotaBytes) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.defaultQuotaBytes = defaultQuotaBytes;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("An account with email " + normalizedEmail + " already exists");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setFullName(request.getFullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_USER);
        user.setStorageQuotaBytes(defaultQuotaBytes);
        user.setStorageUsedBytes(0L);
        user.setIsActive(true);

        User savedUser = userRepository.save(user);
        log.info("Registered new user with ID: {} and email: {}", savedUser.getId(), savedUser.getEmail());

        String token = jwtTokenProvider.generateTokenForUser(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName()
        );

        return new AuthResponse(token, UserSummaryDto.fromEntity(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtTokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", normalizedEmail));

        log.info("User {} successfully authenticated", user.getEmail());
        return new AuthResponse(token, UserSummaryDto.fromEntity(user));
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return UserSummaryDto.fromEntity(user);
    }
}

