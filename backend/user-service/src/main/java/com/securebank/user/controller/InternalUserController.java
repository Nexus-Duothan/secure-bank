package com.securebank.user.controller;

import com.securebank.user.dto.ProvisionProfileRequest;
import com.securebank.user.dto.UserProfileResponse;
import com.securebank.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service to service profile provisioning, called by auth-service when a customer registers.
 *
 * <p>It sits under {@code /internal} on purpose: the API gateway only publishes
 * {@code /api/v1/users/**}, so no browser can reach this and mint a profile.
 */
@RestController
@RequestMapping("/internal/v1/users")
@RequiredArgsConstructor
public class InternalUserController {

  private final UserService userService;

  @PostMapping
  public ResponseEntity<UserProfileResponse> provisionProfile(
    @Valid @RequestBody ProvisionProfileRequest request
  ) {
    return ResponseEntity.ok(userService.provisionProfile(request));
  }
}
