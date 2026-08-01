package com.securebank.user.controller;

import com.securebank.user.dto.ConfirmChangeRequest;
import com.securebank.user.dto.OtpChallengeResponse;
import com.securebank.user.dto.RoleUpdateRequest;
import com.securebank.user.dto.StatusUpdateRequest;
import com.securebank.user.dto.UserProfileResponse;
import com.securebank.user.security.CallerIdentity;
import com.securebank.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Role and status administration (FR-08). Authorisation is enforced in {@link UserService} so the
 * rules hold for any caller of those operations, not only this HTTP surface.
 *
 * <p>Role and status changes are high-risk actions (FR-04): they are staged behind a one-time code
 * sent to the staff member and only applied on {@code /changes/{id}/confirm}. There is no direct
 * mutation endpoint, so the OTP step cannot be skipped.
 */
@RestController
@RequestMapping("/api/v1/users/admin")
@RequiredArgsConstructor
public class AdminUserController {

  private final UserService userService;

  @GetMapping
  public ResponseEntity<List<UserProfileResponse>> getUsers(CallerIdentity caller) {
    return ResponseEntity.ok(userService.getUsers(caller));
  }

  @GetMapping("/{userId}")
  public ResponseEntity<UserProfileResponse> getUser(
    CallerIdentity caller,
    @PathVariable UUID userId
  ) {
    return ResponseEntity.ok(userService.getUser(caller, userId));
  }

  @PostMapping("/{userId}/role-change")
  public ResponseEntity<OtpChallengeResponse> requestRoleChange(
    CallerIdentity caller,
    @PathVariable UUID userId,
    @Valid @RequestBody RoleUpdateRequest request
  ) {
    return ResponseEntity.ok(userService.requestRoleChange(caller, userId, request));
  }

  @PostMapping("/{userId}/status-change")
  public ResponseEntity<OtpChallengeResponse> requestStatusChange(
    CallerIdentity caller,
    @PathVariable UUID userId,
    @Valid @RequestBody StatusUpdateRequest request
  ) {
    return ResponseEntity.ok(userService.requestStatusChange(caller, userId, request));
  }

  @PostMapping("/changes/{changeRequestId}/confirm")
  public ResponseEntity<UserProfileResponse> confirmChange(
    CallerIdentity caller,
    @PathVariable UUID changeRequestId,
    @Valid @RequestBody ConfirmChangeRequest request
  ) {
    return ResponseEntity.ok(userService.confirmAdminChange(caller, changeRequestId, request));
  }
}
