package com.securebank.auth.controller;

import com.securebank.auth.dto.CredentialAccessUpdateRequest;
import com.securebank.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service to service updates to a user's sign-in role and status, called by user-service after an
 * administrator has confirmed the change with their authenticator code.
 *
 * <p>It sits under {@code /internal} on purpose: the API gateway only publishes
 * {@code /api/v1/auth/**}, so no browser can reach this and grant itself a role.
 */
@RestController
@RequestMapping("/internal/v1/credentials")
@RequiredArgsConstructor
public class InternalCredentialController {

  private final AuthService authService;

  @PutMapping("/{userId}/access")
  public ResponseEntity<Map<String, String>> updateAccess(
    @PathVariable UUID userId,
    @Valid @RequestBody CredentialAccessUpdateRequest request
  ) {
    authService.updateAccess(userId, request);
    return ResponseEntity.ok(Map.of("message", "Sign-in access updated"));
  }
}
