package com.securebank.user.controller;

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

  @PatchMapping("/{userId}/role")
  public ResponseEntity<UserProfileResponse> updateRole(
    CallerIdentity caller,
    @PathVariable UUID userId,
    @Valid @RequestBody RoleUpdateRequest request
  ) {
    return ResponseEntity.ok(userService.updateRole(caller, userId, request));
  }

  @PatchMapping("/{userId}/status")
  public ResponseEntity<UserProfileResponse> updateStatus(
    CallerIdentity caller,
    @PathVariable UUID userId,
    @Valid @RequestBody StatusUpdateRequest request
  ) {
    return ResponseEntity.ok(userService.updateStatus(caller, userId, request));
  }
}
