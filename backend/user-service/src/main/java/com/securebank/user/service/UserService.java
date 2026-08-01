package com.securebank.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.user.config.UserServiceProperties;
import com.securebank.user.dto.*;
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
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile management (FR-07) and role administration (FR-08).
 *
 * <p>Every self-service mutation is staged as a {@link PendingUserChange} and only applied once the
 * customer confirms it with a one-time code, so a hijacked session alone cannot alter contact
 * details or linked devices.
 */
@Service
@RequiredArgsConstructor
public class UserService {

  private static final SecureRandom OTP_RANDOM = new SecureRandom();
  private static final int OTP_BOUND = 1_000_000;
  private static final int MAX_TRUSTED_DEVICES = 3;

  private final UserProfileRepository userProfileRepository;
  private final UserDeviceRepository userDeviceRepository;
  private final PendingUserChangeRepository pendingUserChangeRepository;
  private final ObjectMapper objectMapper;
  private final PasswordEncoder otpEncoder;
  private final UserServiceProperties properties;
  private final UserSecurityAlertService userSecurityAlertService;

  // --------------------------------------------------------------------
  // Self-service profile (FR-07)
  // --------------------------------------------------------------------

  @Transactional(readOnly = true)
  public UserProfileResponse getProfile(CallerIdentity caller) {
    return toProfileResponse(findUser(caller.userId()));
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

  @Transactional
  public OtpChallengeResponse requestAccountFreeze(
    CallerIdentity caller,
    FreezeAccountRequest request
  ) {
    UserProfile profile = findUser(caller.userId());
    if (profile.isFrozen()) {
      throw new ConflictException("Account is already frozen");
    }
    return createChallenge(profile, ChangeRequestType.FREEZE_ACCOUNT, request);
  }

  @Transactional
  public OtpChallengeResponse requestAccountUnfreeze(CallerIdentity caller) {
    UserProfile profile = findUser(caller.userId());
    if (!profile.isFrozen()) {
      throw new ConflictException("Account is not frozen");
    }
    return createChallenge(profile, ChangeRequestType.UNFREEZE_ACCOUNT, null);
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

    verifyOtp(change, request.otpCode());

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
    applyStatus(profile, request.status(), "Set to " + request.status() + " by " + caller.role());
    return toProfileResponse(userProfileRepository.save(profile));
  }

  // --------------------------------------------------------------------
  // OTP challenge handling
  // --------------------------------------------------------------------

  private OtpChallengeResponse createChallenge(
    UserProfile profile,
    ChangeRequestType type,
    Object payload
  ) {
    String code = generateOtpCode();
    PendingUserChange saved = pendingUserChangeRepository.save(
      PendingUserChange.builder()
        .userProfileId(profile.getId())
        .type(type)
        .payloadJson(writePayload(payload))
        .otpHash(otpEncoder.encode(code))
        .expiresAt(Instant.now().plus(properties.otp().ttl()))
        .build()
    );

    // Delivery over the customer's preferred channel is the Notification Service's job (FR-29);
    // until that hop exists the code is only echoed back when explicitly enabled for local demos.
    return new OtpChallengeResponse(
      saved.getId(),
      type,
      maskEmail(profile.getEmail()),
      saved.getExpiresAt(),
      "Enter the six digit code sent to your registered contact to confirm this change.",
      properties.otp().exposeCode() ? code : null
    );
  }

  private void verifyOtp(PendingUserChange change, String submittedCode) {
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
      throw new OtpVerificationException("OTP challenge expired");
    }
    if (!otpEncoder.matches(submittedCode, change.getOtpHash())) {
      change.setFailedAttempts(change.getFailedAttempts() + 1);
      pendingUserChangeRepository.save(change);
      int remaining = Math.max(0, maxAttempts - change.getFailedAttempts());
      throw new OtpVerificationException(
        "Invalid OTP code, " + remaining + " attempt(s) remaining"
      );
    }
  }

  private String generateOtpCode() {
    return "%06d".formatted(OTP_RANDOM.nextInt(OTP_BOUND));
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
        case FREEZE_ACCOUNT -> applyStatus(
          profile,
          UserStatus.FROZEN,
          objectMapper.readValue(change.getPayloadJson(), FreezeAccountRequest.class).reason()
        );
        case UNFREEZE_ACCOUNT -> applyStatus(profile, UserStatus.ACTIVE, null);
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

  private void applyStatus(UserProfile profile, UserStatus status, String reason) {
    boolean freezing = status == UserStatus.FROZEN;
    profile.setStatus(status);
    profile.setFrozen(freezing);
    if (freezing) {
      if (profile.getFrozenAt() == null) {
        profile.setFrozenAt(Instant.now());
      }
      profile.setFreezeReason(reason);
    } else {
      profile.setFrozenAt(null);
      profile.setFreezeReason(null);
    }
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
      profile.isFrozen(),
      profile.getFreezeReason(),
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
      case FREEZE_ACCOUNT -> "An account freeze request was confirmed for customer protection.";
      case UNFREEZE_ACCOUNT -> "The account was reactivated after successful OTP confirmation.";
    };
  }
}
