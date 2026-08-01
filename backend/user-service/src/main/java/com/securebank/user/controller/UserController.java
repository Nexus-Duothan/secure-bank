package com.securebank.user.controller;

import com.securebank.user.dto.*;
import com.securebank.user.security.CallerIdentity;
import com.securebank.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Self-service profile endpoints (FR-07). Every route acts on the {@link CallerIdentity} resolved
 * from the gateway identity headers, never on a request-supplied user id.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("/me")
  public ResponseEntity<UserProfileResponse> getCurrentProfile(CallerIdentity caller) {
    return ResponseEntity.ok(userService.getProfile(caller));
  }

  @PostMapping("/me/profile-change")
  public ResponseEntity<OtpChallengeResponse> requestProfileUpdate(
    CallerIdentity caller,
    @Valid @RequestBody ProfileUpdateRequest request
  ) {
    return ResponseEntity.ok(userService.requestProfileUpdate(caller, request));
  }

  @PostMapping("/me/notification-preferences-change")
  public ResponseEntity<OtpChallengeResponse> requestNotificationPreferenceUpdate(
    CallerIdentity caller,
    @Valid @RequestBody NotificationPreferencesUpdateRequest request
  ) {
    return ResponseEntity.ok(userService.requestNotificationPreferenceUpdate(caller, request));
  }

  @PostMapping("/me/devices/link")
  public ResponseEntity<OtpChallengeResponse> requestDeviceLink(
    CallerIdentity caller,
    @Valid @RequestBody DeviceLinkRequest request
  ) {
    return ResponseEntity.ok(userService.requestDeviceLink(caller, request));
  }

  @PostMapping("/me/devices/trust")
  public ResponseEntity<OtpChallengeResponse> requestDeviceTrust(
    CallerIdentity caller,
    @Valid @RequestBody DeviceActionRequest request
  ) {
    return ResponseEntity.ok(userService.requestDeviceTrust(caller, request));
  }

  @PostMapping("/me/devices/revoke")
  public ResponseEntity<OtpChallengeResponse> requestDeviceRevoke(
    CallerIdentity caller,
    @Valid @RequestBody DeviceActionRequest request
  ) {
    return ResponseEntity.ok(userService.requestDeviceRevoke(caller, request));
  }

  @PostMapping("/me/freeze")
  public ResponseEntity<OtpChallengeResponse> requestAccountFreeze(
    CallerIdentity caller,
    @Valid @RequestBody FreezeAccountRequest request
  ) {
    return ResponseEntity.ok(userService.requestAccountFreeze(caller, request));
  }

  @PostMapping("/me/unfreeze")
  public ResponseEntity<OtpChallengeResponse> requestAccountUnfreeze(CallerIdentity caller) {
    return ResponseEntity.ok(userService.requestAccountUnfreeze(caller));
  }

  @PostMapping("/me/changes/{changeRequestId}/confirm")
  public ResponseEntity<UserProfileResponse> confirmChange(
    CallerIdentity caller,
    @PathVariable UUID changeRequestId,
    @Valid @RequestBody ConfirmChangeRequest request
  ) {
    return ResponseEntity.ok(userService.confirmChange(caller, changeRequestId, request));
  }
}
