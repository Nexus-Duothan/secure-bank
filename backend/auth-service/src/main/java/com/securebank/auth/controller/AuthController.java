package com.securebank.auth.controller;

import com.securebank.auth.dto.*;
import com.securebank.auth.service.AuthService;
import com.securebank.auth.service.KycService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final KycService kycService;

  @PostMapping("/register")
  public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
    RegisterResponse response = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/verify-kyc")
  public ResponseEntity<KycApplicationResponse> submitKyc(
    Authentication authentication,
    @Valid @RequestBody KycSubmissionRequest request
  ) {
    UUID userId = UUID.fromString(authentication.getName());
    KycApplicationResponse response = kycService.submitKyc(userId, request);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/officer/kyc/pending")
  @PreAuthorize("hasAnyRole('BANK_OFFICER', 'ADMIN')")
  public ResponseEntity<List<KycApplicationResponse>> getPendingKycApplications() {
    List<KycApplicationResponse> pendingList = kycService.getPendingApplications();
    return ResponseEntity.ok(pendingList);
  }

  @PostMapping("/officer/kyc/{applicationId}/review")
  @PreAuthorize("hasAnyRole('BANK_OFFICER', 'ADMIN')")
  public ResponseEntity<KycApplicationResponse> reviewKycApplication(
    Authentication authentication,
    @PathVariable UUID applicationId,
    @Valid @RequestBody OfficerKycReviewRequest request
  ) {
    String officerUsername = authentication.getName();
    KycApplicationResponse response = kycService.reviewKycApplication(
      applicationId,
      officerUsername,
      request
    );
    return ResponseEntity.ok(response);
  }

  @PostMapping("/login")
  public ResponseEntity<PreAuthResponse> login(@Valid @RequestBody LoginRequest request) {
    PreAuthResponse response = authService.login(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/login/verify-mfa")
  public ResponseEntity<AuthTokenResponse> verifyMfa(
    @Valid @RequestBody MfaVerifyRequest request,
    HttpServletRequest httpRequest
  ) {
    String ipAddress = httpRequest.getRemoteAddr();
    String userAgent = httpRequest.getHeader("User-Agent");
    AuthTokenResponse response = authService.verifyMfa(request, ipAddress, userAgent);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/resend-otp")
  public ResponseEntity<Map<String, Object>> resendOtp(
    @Valid @RequestBody ResendOtpRequest request
  ) {
    return ResponseEntity.ok(authService.resendOtp(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthTokenResponse> refreshToken(
    @RequestBody Map<String, String> body,
    HttpServletRequest httpRequest
  ) {
    String refreshToken = body.get("refreshToken");
    if (refreshToken == null || refreshToken.isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    String ipAddress = httpRequest.getRemoteAddr();
    String userAgent = httpRequest.getHeader("User-Agent");
    AuthTokenResponse response = authService.refresh(refreshToken, ipAddress, userAgent);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/sessions")
  public ResponseEntity<List<SessionResponse>> getActiveSessions(
    Authentication authentication,
    @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken
  ) {
    UUID userId = UUID.fromString(authentication.getName());
    List<SessionResponse> sessions = authService.getActiveSessions(userId, refreshToken);
    return ResponseEntity.ok(sessions);
  }

  @DeleteMapping("/sessions/{sessionId}")
  public ResponseEntity<Void> revokeSession(
    Authentication authentication,
    @PathVariable UUID sessionId
  ) {
    UUID userId = UUID.fromString(authentication.getName());
    authService.revokeSession(userId, sessionId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/password-reset/request")
  public ResponseEntity<Map<String, String>> requestPasswordReset(
    @Valid @RequestBody PasswordResetRequest request
  ) {
    String token = authService.requestPasswordReset(request);
    return ResponseEntity.ok(Map.of("message", "Password reset token generated", "token", token));
  }

  @PostMapping("/password-reset/confirm")
  public ResponseEntity<Map<String, String>> confirmPasswordReset(
    @Valid @RequestBody PasswordResetConfirmRequest request
  ) {
    authService.confirmPasswordReset(request);
    return ResponseEntity.ok(
      Map.of("message", "Password reset successful. All existing sessions have been revoked.")
    );
  }

  @GetMapping("/validate")
  public ResponseEntity<ValidateTokenResponse> validateToken(@RequestParam("token") String token) {
    ValidateTokenResponse response = authService.validateToken(token);
    return ResponseEntity.ok(response);
  }
}
