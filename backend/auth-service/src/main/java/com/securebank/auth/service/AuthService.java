package com.securebank.auth.service;

import com.securebank.auth.dto.*;
import com.securebank.auth.entity.PasswordResetToken;
import com.securebank.auth.entity.UserCredential;
import com.securebank.auth.entity.UserSession;
import com.securebank.auth.enums.Role;
import com.securebank.auth.enums.UserStatus;
import com.securebank.auth.repository.PasswordResetTokenRepository;
import com.securebank.auth.repository.UserCredentialRepository;
import com.securebank.auth.repository.UserSessionRepository;
import com.securebank.auth.security.JwtTokenProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserCredentialRepository userRepository;
  private final UserSessionRepository sessionRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider tokenProvider;

  @Transactional
  public RegisterResponse register(RegisterRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new IllegalArgumentException("Username is already taken");
    }
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new IllegalArgumentException("Email is already registered");
    }

    Role role = request.getRole() != null ? request.getRole() : Role.CUSTOMER;

    UserCredential user = UserCredential.builder()
      .username(request.getUsername())
      .email(request.getEmail())
      .passwordHash(passwordEncoder.encode(request.getPassword()))
      .nationalIdOrPassport(request.getNationalIdOrPassport())
      .fullName(request.getFullName())
      .role(role)
      .status(UserStatus.PENDING_KYC)
      .mfaEnabled(true)
      .build();

    UserCredential saved = userRepository.save(user);

    return RegisterResponse.builder()
      .userId(saved.getId())
      .username(saved.getUsername())
      .email(saved.getEmail())
      .role(saved.getRole())
      .status(saved.getStatus())
      .message("User registered successfully. Please submit KYC documentation.")
      .build();
  }

  @Transactional(readOnly = true)
  public PreAuthResponse login(LoginRequest request) {
    UserCredential user = userRepository
      .findByUsername(request.getUsernameOrEmail())
      .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
      .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid username or password");
    }

    if (user.getStatus() == UserStatus.BLOCKED || user.getStatus() == UserStatus.FROZEN) {
      throw new IllegalStateException(
        "Account is " + user.getStatus().name().toLowerCase() + ". Contact customer support."
      );
    }

    String preAuthToken = tokenProvider.generatePreAuthToken(user.getId(), user.getUsername());

    return PreAuthResponse.builder()
      .preAuthToken(preAuthToken)
      .mfaRequired(user.isMfaEnabled())
      .message("Primary credentials verified. TOTP verification required.")
      .build();
  }

  @Transactional
  public AuthTokenResponse verifyMfa(MfaVerifyRequest request, String ipAddress, String userAgent) {
    if (!tokenProvider.validateToken(request.getPreAuthToken())) {
      throw new IllegalArgumentException("Invalid or expired pre-authentication token");
    }

    String tokenType = tokenProvider.getTokenTypeFromToken(request.getPreAuthToken());
    if (!"PRE_AUTH".equals(tokenType)) {
      throw new IllegalArgumentException("Invalid token type");
    }

    UUID userId = tokenProvider.getUserIdFromToken(request.getPreAuthToken());
    UserCredential user = userRepository
      .findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("User not found"));

    // Validate TOTP code (Standard verification simulation / 6-digit TOTP)
    if (request.getTotpCode() == null || request.getTotpCode().length() != 6) {
      throw new IllegalArgumentException("Invalid 6-digit TOTP code");
    }

    return createSessionAndGenerateTokens(user, ipAddress, userAgent);
  }

  @Transactional
  public AuthTokenResponse refresh(String refreshToken, String ipAddress, String userAgent) {
    if (!tokenProvider.validateToken(refreshToken)) {
      throw new IllegalArgumentException("Invalid or expired refresh token");
    }

    UserSession session = sessionRepository
      .findByRefreshTokenAndRevokedFalse(refreshToken)
      .orElseThrow(() -> new IllegalArgumentException("Refresh token is revoked or invalid"));

    if (session.getExpiresAt().isBefore(Instant.now())) {
      session.setRevoked(true);
      sessionRepository.save(session);
      throw new IllegalArgumentException("Refresh token has expired");
    }

    UserCredential user = userRepository
      .findById(session.getUserId())
      .orElseThrow(() -> new IllegalArgumentException("User not found"));

    // Revoke old session token and issue new
    session.setRevoked(true);
    sessionRepository.save(session);

    return createSessionAndGenerateTokens(user, ipAddress, userAgent);
  }

  @Transactional(readOnly = true)
  public List<SessionResponse> getActiveSessions(UUID userId, String currentRefreshToken) {
    return sessionRepository
      .findByUserIdAndRevokedFalse(userId)
      .stream()
      .map(session ->
        SessionResponse.builder()
          .sessionId(session.getId())
          .ipAddress(session.getIpAddress())
          .userAgent(session.getUserAgent())
          .deviceInfo(session.getDeviceInfo())
          .createdAt(session.getCreatedAt())
          .lastActiveAt(session.getLastActiveAt())
          .expiresAt(session.getExpiresAt())
          .current(session.getRefreshToken().equals(currentRefreshToken))
          .build()
      )
      .toList();
  }

  @Transactional
  public void revokeSession(UUID userId, UUID sessionId) {
    UserSession session = sessionRepository
      .findByIdAndUserId(sessionId, userId)
      .orElseThrow(() -> new IllegalArgumentException("Session not found"));
    session.setRevoked(true);
    sessionRepository.save(session);
  }

  @Transactional
  public String requestPasswordReset(PasswordResetRequest request) {
    UserCredential user = userRepository
      .findByEmail(request.getEmail())
      .orElseThrow(() -> new IllegalArgumentException("No user found with the provided email"));

    String resetToken = UUID.randomUUID().toString();
    PasswordResetToken tokenEntity = PasswordResetToken.builder()
      .userId(user.getId())
      .token(resetToken)
      .expiryDate(Instant.now().plusSeconds(900)) // 15 minutes
      .used(false)
      .build();

    passwordResetTokenRepository.save(tokenEntity);
    return resetToken;
  }

  @Transactional
  public void confirmPasswordReset(PasswordResetConfirmRequest request) {
    PasswordResetToken resetToken = passwordResetTokenRepository
      .findByTokenAndUsedFalse(request.getToken())
      .orElseThrow(() -> new IllegalArgumentException("Invalid or used password reset token"));

    if (resetToken.getExpiryDate().isBefore(Instant.now())) {
      throw new IllegalArgumentException("Password reset token has expired");
    }

    if (request.getTotpCode() == null || request.getTotpCode().length() != 6) {
      throw new IllegalArgumentException("MFA TOTP code is required for password reset");
    }

    UserCredential user = userRepository
      .findById(resetToken.getUserId())
      .orElseThrow(() -> new IllegalArgumentException("User not found"));

    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);

    resetToken.setUsed(true);
    passwordResetTokenRepository.save(resetToken);

    // Revoke all active sessions for security
    List<UserSession> activeSessions = sessionRepository.findByUserIdAndRevokedFalse(user.getId());
    activeSessions.forEach(session -> session.setRevoked(true));
    sessionRepository.saveAll(activeSessions);
  }

  @Transactional(readOnly = true)
  public ValidateTokenResponse validateToken(String token) {
    if (!tokenProvider.validateToken(token)) {
      return ValidateTokenResponse.builder().valid(false).build();
    }

    UUID userId = tokenProvider.getUserIdFromToken(token);
    String username = tokenProvider.getUsernameFromToken(token);
    Role role = tokenProvider.getRoleFromToken(token);
    UserStatus status = tokenProvider.getStatusFromToken(token);

    return ValidateTokenResponse.builder()
      .valid(true)
      .userId(userId)
      .username(username)
      .role(role)
      .status(status)
      .build();
  }

  private AuthTokenResponse createSessionAndGenerateTokens(
    UserCredential user,
    String ipAddress,
    String userAgent
  ) {
    String accessToken = tokenProvider.generateAccessToken(
      user.getId(),
      user.getUsername(),
      user.getRole(),
      user.getStatus()
    );
    String refreshToken = tokenProvider.generateRefreshToken(user.getId());

    UserSession session = UserSession.builder()
      .userId(user.getId())
      .sessionTokenHash(passwordEncoder.encode(accessToken))
      .refreshToken(refreshToken)
      .ipAddress(ipAddress)
      .userAgent(userAgent)
      .deviceInfo(parseDeviceInfo(userAgent))
      .expiresAt(Instant.now().plusSeconds(604800)) // 7 days
      .revoked(false)
      .build();

    sessionRepository.save(session);

    return AuthTokenResponse.builder()
      .accessToken(accessToken)
      .refreshToken(refreshToken)
      .tokenType("Bearer")
      .expiresInSeconds(tokenProvider.getAccessTokenExpirationSeconds())
      .userId(user.getId())
      .username(user.getUsername())
      .role(user.getRole())
      .status(user.getStatus())
      .build();
  }

  private String parseDeviceInfo(String userAgent) {
    if (userAgent == null) return "Unknown Device";
    if (userAgent.contains("Mobile")) return "Mobile Browser";
    if (userAgent.contains("Chrome")) return "Chrome Desktop";
    if (userAgent.contains("Firefox")) return "Firefox Desktop";
    return "Desktop Device";
  }
}
