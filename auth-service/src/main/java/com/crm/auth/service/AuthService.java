package com.crm.auth.service;

import com.crm.auth.dto.*;
import com.crm.auth.entity.PasswordResetToken;
import com.crm.auth.entity.Role;
import com.crm.auth.entity.User;
import com.crm.auth.repository.PasswordResetTokenRepository;
import com.crm.auth.repository.RoleRepository;
import com.crm.auth.repository.UserRepository;
import com.crm.common.event.EventPublisher;
import com.crm.common.exception.BadRequestException;
import com.crm.common.exception.DuplicateResourceException;
import com.crm.common.exception.ResourceNotFoundException;
import com.crm.common.exception.UnauthorizedException;
import com.crm.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EventPublisher eventPublisher;
    private final EmailService emailService;
    private final TenantPlanService tenantPlanService;
    private final MfaService mfaService;

    @Value("${app.password-reset.token-expiration-minutes:30}")
    private int tokenExpirationMinutes;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering user with email: {} for tenant: {}", request.getEmail(), request.getTenantId());

        if (userRepository.existsByEmailAndTenantId(request.getEmail(), request.getTenantId())) {
            throw new DuplicateResourceException("User with email " + request.getEmail() + " already exists");
        }

        Role defaultRole = roleRepository.findByNameAndTenantId("USER", request.getTenantId())
                .orElseGet(() -> {
                    Role role = Role.builder()
                            .name("USER")
                            .description("Default user role")
                            .tenantId(request.getTenantId())
                            .build();
                    return roleRepository.save(role);
                });

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .enabled(true)
                .roles(Set.of(defaultRole))
                .build();
        user.setTenantId(request.getTenantId());

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getId());

        // Auto-create FREE plan for new tenants
        tenantPlanService.ensurePlanExists(request.getTenantId());

        eventPublisher.publish("user-events", request.getTenantId(),
                savedUser.getId().toString(), "User", savedUser.getId().toString(),
                "USER_REGISTERED", null);

        List<String> roles = savedUser.getRoles().stream().map(Role::getName).toList();
        String accessToken = jwtTokenProvider.generateToken(
                savedUser.getId().toString(), savedUser.getTenantId(), savedUser.getEmail(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                savedUser.getId().toString(), savedUser.getTenantId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(savedUser.getId().toString())
                .email(savedUser.getEmail())
                .tenantId(savedUser.getTenantId())
                .roles(roles)
                .planName(tenantPlanService.getPlan(savedUser.getTenantId()).getPlanName())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {} tenant: {}", request.getEmail(), request.getTenantId());

        User user = userRepository.findByEmailAndTenantIdAndDeletedFalse(
                        request.getEmail(), request.getTenantId())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new UnauthorizedException("User account is disabled");
        }

        // Check if user has MFA enabled
        if (mfaService.isMfaEnabled(user.getId(), request.getTenantId())) {
            log.info("MFA required for user: {}", user.getId());
            // Issue a short-lived MFA token (not a full access token)
            String mfaToken = jwtTokenProvider.generateRefreshToken(
                    user.getId().toString(), user.getTenantId());
            return AuthResponse.builder()
                    .mfaRequired(true)
                    .mfaToken(mfaToken)
                    .userId(user.getId().toString())
                    .email(user.getEmail())
                    .tenantId(user.getTenantId())
                    .build();
        }

        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String accessToken = jwtTokenProvider.generateToken(
                user.getId().toString(), user.getTenantId(), user.getEmail(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId().toString(), user.getTenantId());

        log.info("User logged in successfully: {}", user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId().toString())
                .email(user.getEmail())
                .tenantId(user.getTenantId())
                .roles(roles)
                .planName(tenantPlanService.getPlan(user.getTenantId()).getPlanName())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse verifyMfa(MfaVerifyRequest request) {
        log.info("MFA verification for user: {}", request.getUserId());

        UUID userId = UUID.fromString(request.getUserId());
        User user = userRepository.findByIdAndTenantIdAndDeletedFalse(userId, request.getTenantId())
                .orElseThrow(() -> new UnauthorizedException("Invalid user"));

        if (!mfaService.verifyCode(userId, request.getTenantId(), request.getCode())) {
            throw new UnauthorizedException("Invalid MFA code");
        }

        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        String accessToken = jwtTokenProvider.generateToken(
                user.getId().toString(), user.getTenantId(), user.getEmail(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId().toString(), user.getTenantId());

        log.info("MFA verified, user logged in: {}", user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId().toString())
                .email(user.getEmail())
                .tenantId(user.getTenantId())
                .roles(roles)
                .planName(tenantPlanService.getPlan(user.getTenantId()).getPlanName())
                .build();
    }

    @Transactional
    public void assignRole(UUID userId, String roleName, String tenantId) {
        log.info("Assigning role {} to user {} for tenant {}", roleName, userId, tenantId);

        User user = userRepository.findByIdAndTenantIdAndDeletedFalse(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Role role = roleRepository.findByNameAndTenantId(roleName, tenantId)
                .orElseGet(() -> {
                    Role newRole = Role.builder()
                            .name(roleName)
                            .description(roleName + " role")
                            .tenantId(tenantId)
                            .build();
                    return roleRepository.save(newRole);
                });

        user.getRoles().add(role);
        userRepository.save(user);
        log.info("Role {} assigned to user {}", roleName, userId);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId, String tenantId) {
        User user = userRepository.findByIdAndTenantIdAndDeletedFalse(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .tenantId(user.getTenantId())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .build();
    }

    // ── Forgot / Reset Password ─────────────────────────────────────

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Password reset requested for email: {} tenant: {}", request.getEmail(), request.getTenantId());

        // Always return success to avoid email enumeration attacks
        userRepository.findByEmailAndTenantIdAndDeletedFalse(request.getEmail(), request.getTenantId())
                .ifPresent(user -> {
                    // Invalidate any existing tokens for this user
                    resetTokenRepository.invalidateAllTokensForUser(user.getId());

                    // Generate a new token
                    String token = UUID.randomUUID().toString();
                    PasswordResetToken resetToken = PasswordResetToken.builder()
                            .user(user)
                            .token(token)
                            .expiresAt(LocalDateTime.now().plusMinutes(tokenExpirationMinutes))
                            .tenantId(user.getTenantId())
                            .build();
                    resetTokenRepository.save(resetToken);

                    // Send reset email
                    emailService.sendPasswordResetEmail(user.getEmail(), token, user.getFirstName());
                    log.info("Password reset email sent to {}", user.getEmail());
                });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset with token");

        PasswordResetToken resetToken = resetTokenRepository.findByTokenAndUsedFalse(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (resetToken.isExpired()) {
            throw new BadRequestException("Reset token has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);

        // Invalidate all other tokens for this user
        resetTokenRepository.invalidateAllTokensForUser(user.getId());

        log.info("Password reset successfully for user {}", user.getId());

        eventPublisher.publish("user-events", user.getTenantId(),
                user.getId().toString(), "User", user.getId().toString(),
                "PASSWORD_RESET", null);
    }
}
