package com.securebank.auth.service;

import com.securebank.auth.client.TotpClient;
import com.securebank.auth.client.UserProfileProvisioningClient;
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
import com.securebank.auth.service.notification.LoginSmsAlertService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private static final String DEMO_REGISTRATION_OTP = "123456";
  private static final int TOTP_MAX_ATTEMPTS = 5;
  private static final long PASSWORD_CHANGE_TTL_SECONDS = 300;
  /** Codes come from the authenticator app, so there is no phone number or inbox to name. */
  private static final String TOTP_DELIVERY_TARGET = "Authenticator app";

  /**
   * Password changes waiting for their authenticator code. Held in memory only: an in-flight
   * confirmation is not customer data, and losing it on restart just means starting again.
   */
  private final ConcurrentMap<UUID, PendingPasswordChange> pendingPasswordChanges =
    new ConcurrentHashMap<>();

  private final UserCredentialRepository userRepository;
  private final UserSessionRepository sessionRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider tokenProvider;
  private final LoginSmsAlertService loginSmsAlertService;
  private final TotpClient totpClient;
  private final UserProfileProvisioningClient userProfileProvisioningClient;

  @Value("${frontend.url:http://localhost:3000}")
  private String frontendUrl;

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
      .phoneNumber(request.getPhoneNumber())
      .role(role)
      .status(UserStatus.PENDING_KYC)
      .mfaEnabled(true)
      .build();

    UserCredential saved = userRepository.save(user);
    // Give the customer a real profile straight away, so the first sign-in has details to read.
    userProfileProvisioningClient.provision(saved);

    loginSmsAlertService.sendRegistrationOtpChallenge(
      saved.getId(),
      saved.getFullName(),
      saved.getPhoneNumber(),
      saved.getEmail(),
      DEMO_REGISTRATION_OTP
    );

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
  public java.util.Map<String, Object> verifyRegistrationPhone(
    RegistrationOtpVerifyRequest request
  ) {
    UserCredential user = userRepository
      .findById(request.getUserId())
      .orElseThrow(() -> new IllegalArgumentException("User not found for phone verification"));

    if (!DEMO_REGISTRATION_OTP.equals(request.getCode())) {
      throw new IllegalArgumentException("Invalid or expired SMS verification code");
    }

    return java.util.Map.of(
      "success",
      true,
      "userId",
      user.getId(),
      "username",
      user.getUsername(),
      "email",
      user.getEmail(),
      "message",
      "Mobile number verified. Continue with authenticator setup."
    );
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
    UserCredential user = null;
    if (
      request.getPreAuthToken() != null && tokenProvider.validateToken(request.getPreAuthToken())
    ) {
      String tokenType = tokenProvider.getTokenTypeFromToken(request.getPreAuthToken());
      if ("PRE_AUTH".equals(tokenType)) {
        UUID userId = tokenProvider.getUserIdFromToken(request.getPreAuthToken());
        user = userRepository.findById(userId).orElse(null);
      }
    }
    if (user == null) {
      throw new IllegalArgumentException("Invalid or expired pre-authentication token");
    }

    // Validate TOTP code via totp-service (with fallback)
    if (!totpClient.verify(user.getId(), request.getTotpCode())) {
      throw new IllegalArgumentException("Invalid 6-digit TOTP code");
    }

    // Catches up anyone whose profile could not be created at registration time; it is a no-op
    // for everyone else.
    userProfileProvisioningClient.provision(user);

    AuthTokenResponse response = createSessionAndGenerateTokens(user, ipAddress, userAgent);
    loginSmsAlertService.sendSuccessfulLoginAlert(
      user.getPhoneNumber(),
      user.getFullName(),
      ipAddress,
      parseDeviceInfo(userAgent),
      Instant.now()
    );
    return response;
  }

  @Transactional(readOnly = true)
  public java.util.Map<String, Object> resendOtp(ResendOtpRequest request) {
    UserCredential user = null;
    if (
      request.getPreAuthToken() != null && tokenProvider.validateToken(request.getPreAuthToken())
    ) {
      UUID userId = tokenProvider.getUserIdFromToken(request.getPreAuthToken());
      user = userRepository.findById(userId).orElse(null);
    }
    if (user == null && request.getPreAuthToken() != null) {
      try {
        UUID userId = UUID.fromString(request.getPreAuthToken());
        user = userRepository.findById(userId).orElse(null);
      } catch (IllegalArgumentException ignored) {}
    }
    if (
      user == null &&
      request.getUsernameOrEmail() != null &&
      !request.getUsernameOrEmail().isBlank()
    ) {
      user = userRepository
        .findByUsername(request.getUsernameOrEmail())
        .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
        .orElse(null);
    }
    if (user == null) {
      throw new IllegalArgumentException("User not found for OTP resend request");
    }

    loginSmsAlertService.sendRegistrationOtpChallenge(
      user.getId(),
      user.getFullName(),
      user.getPhoneNumber(),
      user.getEmail(),
      DEMO_REGISTRATION_OTP
    );

    return java.util.Map.of(
      "success",
      true,
      "message",
      "A new OTP verification code has been dispatched."
    );
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

  // --------------------------------------------------------------------
  // Changing the password while signed in (FR-05)
  // --------------------------------------------------------------------

  /**
   * Checks the current password and stages the new one. Nothing is written to the user yet: the
   * change is held for five minutes and only applied once the authenticator code is confirmed.
   */
  @Transactional(readOnly = true)
  public OtpChallengeResponse requestPasswordChange(UUID userId, PasswordChangeRequest request) {
    UserCredential user = userRepository
      .findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Your current password is incorrect");
    }
    if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Choose a new password you have not used before");
    }

    // Hash now, so the raw password is never held while the change waits for its code.
    UUID changeRequestId = UUID.randomUUID();
    Instant expiresAt = Instant.now().plusSeconds(PASSWORD_CHANGE_TTL_SECONDS);
    pendingPasswordChanges.put(
      changeRequestId,
      new PendingPasswordChange(userId, passwordEncoder.encode(request.getNewPassword()), expiresAt)
    );

    return new OtpChallengeResponse(
      changeRequestId,
      "CHANGE_PASSWORD",
      TOTP_DELIVERY_TARGET,
      expiresAt,
      "Enter the current six digit code from your authenticator app to change your password.",
      null
    );
  }

  /**
   * Applies a staged password change and revokes every session, including the caller's own, so the
   * customer signs in again with the new password.
   */
  @Transactional
  public void confirmPasswordChange(
    UUID userId,
    UUID changeRequestId,
    PasswordChangeConfirmRequest request
  ) {
    PendingPasswordChange pending = pendingPasswordChanges.get(changeRequestId);
    if (pending == null || !pending.userId().equals(userId)) {
      throw new IllegalArgumentException("Password change request not found");
    }
    if (pending.expiresAt().isBefore(Instant.now())) {
      pendingPasswordChanges.remove(changeRequestId);
      throw new IllegalArgumentException("This password change request has expired");
    }
    if (pending.failedAttempts() >= TOTP_MAX_ATTEMPTS) {
      pendingPasswordChanges.remove(changeRequestId);
      throw new IllegalArgumentException(
        "Too many incorrect codes for this request; start the password change again"
      );
    }

    UserCredential user = userRepository
      .findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (!totpClient.verify(user.getId(), request.getTotpCode())) {
      pending.incrementAttempts();
      throw new IllegalArgumentException("Invalid 6-digit TOTP code");
    }

    user.setPasswordHash(pending.passwordHash());
    userRepository.save(user);
    pendingPasswordChanges.remove(changeRequestId);

    revokeAllSessions(user.getId());

    loginSmsAlertService.sendPasswordChangedAlert(
      user.getPhoneNumber(),
      user.getEmail(),
      user.getFullName(),
      Instant.now()
    );
  }

  @Transactional
  public void requestPasswordReset(PasswordResetRequest request) {
    UserCredential user = userRepository
      .findByEmail(request.getEmail())
      .orElseThrow(() -> new IllegalArgumentException("No user found with the provided email"));

    String resetToken = UUID.randomUUID().toString();
    Instant expiresAt = Instant.now().plusSeconds(900);
    PasswordResetToken tokenEntity = PasswordResetToken.builder()
      .userId(user.getId())
      .token(resetToken)
      .expiryDate(expiresAt) // 15 minutes
      .used(false)
      .build();

    passwordResetTokenRepository.save(tokenEntity);
    loginSmsAlertService.sendPasswordResetLink(
      user.getId(),
      user.getEmail(),
      user.getFullName(),
      buildPasswordResetUrl(resetToken),
      expiresAt
    );
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

    if (!totpClient.verify(user.getId(), request.getTotpCode())) {
      throw new IllegalArgumentException("Invalid 6-digit TOTP code");
    }

    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);

    resetToken.setUsed(true);
    passwordResetTokenRepository.save(resetToken);

    revokeAllSessions(user.getId());

    loginSmsAlertService.sendPasswordChangedAlert(
      user.getPhoneNumber(),
      user.getEmail(),
      user.getFullName(),
      Instant.now()
    );
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

  private String buildPasswordResetUrl(String resetToken) {
    String baseUrl = frontendUrl.endsWith("/")
      ? frontendUrl.substring(0, frontendUrl.length() - 1)
      : frontendUrl;
    return baseUrl + "/reset-password/" + resetToken;
  }

  /** Signs the customer out everywhere after their password changes, on every device. */
  private void revokeAllSessions(UUID userId) {
    List<UserSession> activeSessions = sessionRepository.findByUserIdAndRevokedFalse(userId);
    activeSessions.forEach(session -> session.setRevoked(true));
    sessionRepository.saveAll(activeSessions);
  }

  /** A password change waiting for its authenticator code. */
  private static final class PendingPasswordChange {

    private final UUID userId;
    private final String passwordHash;
    private final Instant expiresAt;
    private int failedAttempts;

    private PendingPasswordChange(UUID userId, String passwordHash, Instant expiresAt) {
      this.userId = userId;
      this.passwordHash = passwordHash;
      this.expiresAt = expiresAt;
    }

    private UUID userId() {
      return userId;
    }

    private String passwordHash() {
      return passwordHash;
    }

    private Instant expiresAt() {
      return expiresAt;
    }

    private int failedAttempts() {
      return failedAttempts;
    }

    private void incrementAttempts() {
      this.failedAttempts++;
    }
  }
}
