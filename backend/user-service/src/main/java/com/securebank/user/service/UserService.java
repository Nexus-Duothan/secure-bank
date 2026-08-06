package com.securebank.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.user.client.TotpClient;
import com.securebank.user.config.UserServiceProperties;
import com.securebank.user.dto.AdminRoleChangePayload;
import com.securebank.user.dto.AdminStatusChangePayload;
import com.securebank.user.dto.ConfirmChangeRequest;
import com.securebank.user.dto.DeviceActionRequest;
import com.securebank.user.dto.DeviceLinkRequest;
import com.securebank.user.dto.NotificationPreferencesResponse;
import com.securebank.user.dto.NotificationPreferencesUpdateRequest;
import com.securebank.user.dto.OtpChallengeResponse;
import com.securebank.user.dto.ProfileUpdateRequest;
import com.securebank.user.dto.ProvisionProfileRequest;
import com.securebank.user.dto.RoleUpdateRequest;
import com.securebank.user.dto.StatusUpdateRequest;
import com.securebank.user.dto.UserDeviceResponse;
import com.securebank.user.dto.UserProfileResponse;
import com.securebank.user.entity.PendingUserChange;
import com.securebank.user.entity.UserDevice;
import com.securebank.user.entity.UserProfile;
import com.securebank.user.enums.ChangeRequestType;
import com.securebank.user.enums.Role;
import com.securebank.user.enums.UserStatus;
import com.securebank.user.repository.PendingUserChangeRepository;
import com.securebank.user.repository.UserDeviceRepository;
import com.securebank.user.repository.UserProfileRepository;
import com.securebank.user.security.AccessDeniedException;
import com.securebank.user.security.CallerIdentity;
import com.securebank.user.service.notification.UserSecurityAlertService;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile management (FR-07) and role administration (FR-08).
 *
 * <p>Every self-service mutation is staged as a {@link PendingUserChange} and only applied once the
 * customer confirms it with the current code from their authenticator app (TOTP), so a hijacked
 * session alone cannot alter contact details or linked devices.
 */
@Service
@RequiredArgsConstructor
public class UserService {

  private static final int MAX_TRUSTED_DEVICES = 3;
  /** Shown instead of a masked phone number now that codes come from the authenticator app. */
  private static final String TOTP_DELIVERY_TARGET = "Authenticator app";
  private static final String TOTP_CHALLENGE_MESSAGE =
    "Enter the current six digit code from your authenticator app to confirm this change.";

  private final UserProfileRepository userProfileRepository;
  private final UserDeviceRepository userDeviceRepository;
  private final PendingUserChangeRepository pendingUserChangeRepository;
  private final ObjectMapper objectMapper;
  private final UserServiceProperties properties;
  private final UserSecurityAlertService userSecurityAlertService;
  private final TotpClient totpClient;

  // --------------------------------------------------------------------
  // Self-service profile (FR-07)
  // --------------------------------------------------------------------

  @Transactional(readOnly = true)
  public UserProfileResponse getProfile(CallerIdentity caller) {
    return toProfileResponse(findUser(caller.userId()));
  }

  /**
   * Creates the profile for a customer auth-service has just registered, so their very first
   * sign-in reads a real row rather than nothing. Idempotent: if the profile already exists it is
   * returned untouched, because the customer's own edits must outrank a replayed provisioning call.
   */
  @Transactional
  public UserProfileResponse provisionProfile(ProvisionProfileRequest request) {
    return toProfileResponse(
      userProfileRepository.findById(request.id()).orElseGet(() ->
        userProfileRepository.save(
          UserProfile.builder()
            .id(request.id())
            .fullName(request.fullName())
            .email(request.email())
            .phoneNumber(request.phoneNumber())
            .role(request.role())
            .status(request.status())
            // KYC has not been reviewed yet at registration time.
            .idVerified(request.status() == UserStatus.ACTIVE)
            .build()
        )
      )
    );
  }

  @Transactional
  public OtpChallengeResponse requestProfileUpdate(
    CallerIdentity caller,
    ProfileUpdateRequest request
  ) {
    UserProfile profile = findUser(caller.userId());
    // Reject a taken email now rather than after the customer has typed a code.
    assertEmailAvailable(profile, request.email());
    return createChallenge(profile, ChangeRequestType.UPDATE_PROFILE, request);
  }

  @Transactional
  public OtpChallengeResponse requestNotificationPreferenceUpdate(
    CallerIdentity caller,
    NotificationPreferencesUpdateRequest request
  ) {
    return createChallenge(
      findUser(caller.userId()),
      ChangeRequestType.UPDATE_NOTIFICATION_PREFERENCES,
      request
    );
  }

  @Transactional
  public OtpChallengeResponse requestDeviceLink(CallerIdentity caller, DeviceLinkRequest request) {
    return createChallenge(findUser(caller.userId()), ChangeRequestType.LINK_DEVICE, request);
  }

  @Transactional
  public OtpChallengeResponse requestDeviceTrust(
    CallerIdentity caller,
    DeviceActionRequest request
  ) {
    UserProfile profile = findUser(caller.userId());
    UserDevice device = findDevice(profile.getId(), request.deviceId());
    if (!device.isTrusted() && countTrustedDevices(profile.getId()) >= MAX_TRUSTED_DEVICES) {
      throw new ConflictException("Maximum linked devices reached");
    }
    return createChallenge(profile, ChangeRequestType.TRUST_DEVICE, request);
  }

  @Transactional
  public OtpChallengeResponse requestDeviceRevoke(
    CallerIdentity caller,
    DeviceActionRequest request
  ) {
    UserProfile profile = findUser(caller.userId());
    findDevice(profile.getId(), request.deviceId());
    return createChallenge(profile, ChangeRequestType.REVOKE_DEVICE, request);
  }

  /**
   * Applies a staged change once its one-time code checks out.
   *
   * <p>{@code noRollbackFor} keeps the incremented attempt counter durable on a wrong code; without
   * it the rollback would restore the counter to zero and leave the code brute forceable.
   */
  @Transactional(noRollbackFor = OtpVerificationException.class)
  public UserProfileResponse confirmChange(
    CallerIdentity caller,
    UUID changeRequestId,
    ConfirmChangeRequest request
  ) {
    PendingUserChange change = pendingUserChangeRepository
      .findByIdAndUserProfileId(changeRequestId, caller.userId())
      .orElseThrow(() -> new EntityNotFoundException("Change request not found"));
    if (isAdminChange(change.getType())) {
      // Administrative changes have their own confirm endpoint with staff checks.
      throw new EntityNotFoundException("Change request not found");
    }

    verifyTotp(change, request.otpCode());

    UserProfile profile = findUser(change.getUserProfileId());
    applyChange(profile, change);
    change.setConfirmed(true);
    change.setConfirmedAt(Instant.now());
    pendingUserChangeRepository.save(change);
    UserProfile savedProfile = userProfileRepository.save(profile);
    userSecurityAlertService.sendCriticalChangeAlert(
      savedProfile,
      change.getType(),
      describeConfirmedChange(savedProfile, change.getType())
    );
    return toProfileResponse(savedProfile);
  }

  // --------------------------------------------------------------------
  // Administration (FR-08)
  // --------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<UserProfileResponse> getUsers(CallerIdentity caller) {
    caller.requireAnyRole(Role.ADMIN, Role.BANK_OFFICER);
    return userProfileRepository.findAll().stream().map(this::toProfileResponse).toList();
  }

  @Transactional(readOnly = true)
  public UserProfileResponse getUser(CallerIdentity caller, UUID userId) {
    caller.requireAnyRole(Role.ADMIN, Role.BANK_OFFICER);
    return toProfileResponse(findUser(userId));
  }

  @Transactional
  public UserProfileResponse updateRole(
    CallerIdentity caller,
    UUID userId,
    RoleUpdateRequest request
  ) {
    // Officers may service accounts but must not be able to mint administrators, and nobody may
    // re-grade their own account (NFR-S5, least privilege).
    caller.requireAnyRole(Role.ADMIN);
    if (caller.is(userId)) {
      throw new AccessDeniedException("An administrator cannot change their own role");
    }
    UserProfile profile = findUser(userId);
    profile.setRole(request.role());
    return toProfileResponse(userProfileRepository.save(profile));
  }

  @Transactional
  public UserProfileResponse updateStatus(
    CallerIdentity caller,
    UUID userId,
    StatusUpdateRequest request
  ) {
    caller.requireAnyRole(Role.ADMIN, Role.BANK_OFFICER);
    UserProfile profile = findUser(userId);
    applyUserStatus(profile, request.status());
    return toProfileResponse(userProfileRepository.save(profile));
  }

  /**
   * Stages a role change behind an OTP sent to the administrator performing it (FR-04: TOTP/OTP on
   * high-risk actions). The change only lands via {@link #confirmAdminChange}.
   */
  @Transactional
  public OtpChallengeResponse requestRoleChange(
    CallerIdentity caller,
    UUID userId,
    RoleUpdateRequest request
  ) {
    caller.requireAnyRole(Role.ADMIN);
    if (caller.is(userId)) {
      throw new AccessDeniedException("An administrator cannot change their own role");
    }
    UserProfile target = findUser(userId);
    UserProfile staffProfile = findUser(caller.userId());
    return createChallenge(
      staffProfile,
      ChangeRequestType.ADMIN_UPDATE_ROLE,
      new AdminRoleChangePayload(target.getId(), request.role())
    );
  }

  /** Stages an account status change (hold, suspend, reactivate) behind an OTP for the caller. */
  @Transactional
  public OtpChallengeResponse requestStatusChange(
    CallerIdentity caller,
    UUID userId,
    StatusUpdateRequest request
  ) {
    caller.requireAnyRole(Role.ADMIN, Role.BANK_OFFICER);
    UserProfile target = findUser(userId);
    UserProfile staffProfile = findUser(caller.userId());
    return createChallenge(
      staffProfile,
      ChangeRequestType.ADMIN_UPDATE_STATUS,
      new AdminStatusChangePayload(target.getId(), request.status())
    );
  }

  /**
   * Applies a staged administrative change once the staff member's one-time code checks out. The
   * role checks run again here through {@link #updateRole}/{@link #updateStatus}, so a demoted
   * caller cannot land a change staged while they still held the role.
   */
  @Transactional(noRollbackFor = OtpVerificationException.class)
  public UserProfileResponse confirmAdminChange(
    CallerIdentity caller,
    UUID changeRequestId,
    ConfirmChangeRequest request
  ) {
    PendingUserChange change = pendingUserChangeRepository
      .findByIdAndUserProfileId(changeRequestId, caller.userId())
      .orElseThrow(() -> new EntityNotFoundException("Change request not found"));
    if (!isAdminChange(change.getType())) {
      throw new EntityNotFoundException("Change request not found");
    }

    verifyTotp(change, request.otpCode());

    UserProfileResponse target;
    try {
      target = switch (change.getType()) {
        case ADMIN_UPDATE_ROLE -> {
          AdminRoleChangePayload payload = objectMapper.readValue(
            change.getPayloadJson(),
            AdminRoleChangePayload.class
          );
          yield updateRole(caller, payload.targetUserId(), new RoleUpdateRequest(payload.role()));
        }
        case ADMIN_UPDATE_STATUS -> {
          AdminStatusChangePayload payload = objectMapper.readValue(
            change.getPayloadJson(),
            AdminStatusChangePayload.class
          );
          yield updateStatus(
            caller,
            payload.targetUserId(),
            new StatusUpdateRequest(payload.status())
          );
        }
        default -> throw new EntityNotFoundException("Change request not found");
      };
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to read pending change payload", exception);
    }

    change.setConfirmed(true);
    change.setConfirmedAt(Instant.now());
    pendingUserChangeRepository.save(change);
    userSecurityAlertService.sendCriticalChangeAlert(
      findUser(target.id()),
      change.getType(),
      describeConfirmedChange(findUser(caller.userId()), change.getType())
    );
    return target;
  }

  private boolean isAdminChange(ChangeRequestType type) {
    return (
      type == ChangeRequestType.ADMIN_UPDATE_ROLE || type == ChangeRequestType.ADMIN_UPDATE_STATUS
    );
  }

  // --------------------------------------------------------------------
  // TOTP challenge handling
  // --------------------------------------------------------------------

  /**
   * Stages the change. Nothing is sent anywhere: the code is produced by the authenticator app the
   * customer already enrolled, so there is no secret for this service to generate or store.
   */
  private OtpChallengeResponse createChallenge(
    UserProfile profile,
    ChangeRequestType type,
    Object payload
  ) {
    Instant expiresAt = Instant.now().plus(properties.otp().ttl());
    PendingUserChange saved = pendingUserChangeRepository.save(
      PendingUserChange.builder()
        .userProfileId(profile.getId())
        .type(type)
        .payloadJson(writePayload(payload))
        .expiresAt(expiresAt)
        .build()
    );

    return new OtpChallengeResponse(
      saved.getId(),
      type,
      TOTP_DELIVERY_TARGET,
      saved.getExpiresAt(),
      TOTP_CHALLENGE_MESSAGE,
      null
    );
  }

  private void verifyTotp(PendingUserChange change, String submittedCode) {
    if (change.isConfirmed()) {
      throw new ConflictException("Change request already confirmed");
    }
    int maxAttempts = properties.otp().maxAttempts();
    if (change.getFailedAttempts() >= maxAttempts) {
      throw new OtpVerificationException(
        "Too many incorrect codes for this request; start the change again"
      );
    }
    if (change.isExpired(Instant.now())) {
      throw new OtpVerificationException("Verification request expired");
    }
    if (!totpClient.verify(change.getUserProfileId(), submittedCode)) {
      change.setFailedAttempts(change.getFailedAttempts() + 1);
      pendingUserChangeRepository.save(change);
      int remaining = Math.max(0, maxAttempts - change.getFailedAttempts());
      throw new OtpVerificationException(
        "Invalid authenticator code, " + remaining + " attempt(s) remaining"
      );
    }
  }

  // --------------------------------------------------------------------
  // Change application
  // --------------------------------------------------------------------

  private void applyChange(UserProfile profile, PendingUserChange change) {
    try {
      switch (change.getType()) {
        case UPDATE_PROFILE -> applyProfileUpdate(
          profile,
          objectMapper.readValue(change.getPayloadJson(), ProfileUpdateRequest.class)
        );
        case UPDATE_NOTIFICATION_PREFERENCES -> applyNotificationUpdate(
          profile,
          objectMapper.readValue(
            change.getPayloadJson(),
            NotificationPreferencesUpdateRequest.class
          )
        );
        case LINK_DEVICE -> applyDeviceLink(
          profile,
          objectMapper.readValue(change.getPayloadJson(), DeviceLinkRequest.class)
        );
        case TRUST_DEVICE -> applyDeviceTrust(
          profile,
          objectMapper.readValue(change.getPayloadJson(), DeviceActionRequest.class)
        );
        case REVOKE_DEVICE -> applyDeviceRevoke(
          profile,
          objectMapper.readValue(change.getPayloadJson(), DeviceActionRequest.class)
        );
      }
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to read pending change payload", exception);
    }
  }

  private void applyProfileUpdate(UserProfile profile, ProfileUpdateRequest request) {
    if (request.fullName() != null && !request.fullName().isBlank()) {
      profile.setFullName(request.fullName().trim());
    }
    if (request.email() != null && !request.email().isBlank()) {
      // Re-checked here: another profile may have claimed the address since the challenge was cut.
      assertEmailAvailable(profile, request.email());
      profile.setEmail(request.email().trim());
    }
    if (request.phoneNumber() != null) {
      profile.setPhoneNumber(request.phoneNumber());
    }
    if (request.addressLine() != null) {
      profile.setAddressLine(request.addressLine());
    }
    if (request.city() != null) {
      profile.setCity(request.city());
    }
    if (request.country() != null) {
      profile.setCountry(request.country());
    }
    if (request.language() != null && !request.language().isBlank()) {
      profile.setLanguage(request.language());
    }
  }

  private void applyNotificationUpdate(
    UserProfile profile,
    NotificationPreferencesUpdateRequest request
  ) {
    profile.setEmailNotifications(request.email());
    profile.setSmsNotifications(request.sms());
    profile.setPushNotifications(request.push());
  }

  private void applyDeviceLink(UserProfile profile, DeviceLinkRequest request) {
    profile.addDevice(
      UserDevice.builder()
        .deviceName(request.deviceName())
        .deviceType(request.deviceType())
        .browser(request.browser())
        .location(request.location())
        .trusted(false)
        .lastVerifiedAt(null)
        .build()
    );
  }

  private void applyDeviceTrust(UserProfile profile, DeviceActionRequest request) {
    UserDevice device = findDevice(profile.getId(), request.deviceId());
    device.setTrusted(true);
    device.setLastVerifiedAt(Instant.now());
    userDeviceRepository.save(device);
  }

  private void applyDeviceRevoke(UserProfile profile, DeviceActionRequest request) {
    UserDevice device = findDevice(profile.getId(), request.deviceId());
    device.setTrusted(false);
    device.setRevokedAt(Instant.now());
    userDeviceRepository.save(device);
  }

  private void applyUserStatus(UserProfile profile, UserStatus status) {
    profile.setStatus(status);
  }

  // --------------------------------------------------------------------
  // Lookups and mapping
  // --------------------------------------------------------------------

  private UserProfile findUser(UUID userId) {
    return userProfileRepository
      .findById(userId)
      .orElseThrow(() -> new EntityNotFoundException("User not found"));
  }

  private UserDevice findDevice(UUID userId, UUID deviceId) {
    return userDeviceRepository
      .findByIdAndUserProfileId(deviceId, userId)
      .orElseThrow(() -> new EntityNotFoundException("Device not found"));
  }

  private void assertEmailAvailable(UserProfile profile, String email) {
    if (email == null || email.isBlank()) {
      return;
    }
    if (userProfileRepository.existsByEmailIgnoreCaseAndIdNot(email.trim(), profile.getId())) {
      throw new ConflictException("That email address is already registered");
    }
  }

  private long countTrustedDevices(UUID userId) {
    return userDeviceRepository
      .findByUserProfileIdAndRevokedAtIsNullOrderByLastVerifiedAtDesc(userId)
      .stream()
      .filter(UserDevice::isTrusted)
      .count();
  }

  private UserProfileResponse toProfileResponse(UserProfile profile) {
    List<UserDeviceResponse> devices = userDeviceRepository
      .findByUserProfileIdAndRevokedAtIsNullOrderByLastVerifiedAtDesc(profile.getId())
      .stream()
      .map(this::toDeviceResponse)
      .toList();

    return new UserProfileResponse(
      profile.getId(),
      profile.getFullName(),
      profile.getEmail(),
      profile.getPhoneNumber(),
      profile.getAddressLine(),
      profile.getCity(),
      profile.getCountry(),
      profile.getLanguage(),
      profile.getRole(),
      profile.getStatus(),
      profile.isIdVerified(),
      new NotificationPreferencesResponse(
        profile.isEmailNotifications(),
        profile.isSmsNotifications(),
        profile.isPushNotifications()
      ),
      devices
    );
  }

  private UserDeviceResponse toDeviceResponse(UserDevice device) {
    return new UserDeviceResponse(
      device.getId(),
      device.getDeviceName(),
      device.getDeviceType(),
      device.getBrowser(),
      device.getLocation(),
      device.isTrusted(),
      device.getLastVerifiedAt()
    );
  }

  private String writePayload(Object payload) {
    try {
      return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Unable to write pending change payload", exception);
    }
  }

  private String maskEmail(String email) {
    if (email == null) {
      return null;
    }
    int at = email.indexOf('@');
    if (at <= 2) {
      return email;
    }
    return email.charAt(0) + "***" + email.substring(at - 1);
  }

  private String describeConfirmedChange(UserProfile profile, ChangeRequestType type) {
    return switch (type) {
      case UPDATE_PROFILE -> "Contact details for " +
        profile.getFullName() +
        " were updated after OTP confirmation.";
      case UPDATE_NOTIFICATION_PREFERENCES -> "Notification delivery preferences were updated after OTP confirmation.";
      case LINK_DEVICE -> "A new device sign-in request was recorded and is awaiting verification.";
      case TRUST_DEVICE -> "A new device was verified and linked to the account.";
      case REVOKE_DEVICE -> "A previously linked device was removed from the account.";
      case ADMIN_UPDATE_ROLE -> "The role on this account was changed by " +
        profile.getFullName() +
        " after OTP confirmation.";
      case ADMIN_UPDATE_STATUS -> "The status of this account was changed by " +
        profile.getFullName() +
        " after OTP confirmation.";
    };
  }
}
