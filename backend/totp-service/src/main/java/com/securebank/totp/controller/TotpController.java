package com.securebank.totp.controller;

import com.securebank.totp.dto.*;
import com.securebank.totp.service.TotpService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/totp")
@RequiredArgsConstructor
public class TotpController {

  private final TotpService totpService;

  @PostMapping("/setup/{userId}")
  public ResponseEntity<TotpSetupResponse> setupTotp(
    @PathVariable UUID userId,
    @RequestParam(value = "username", required = false) String username
  ) {
    TotpSetupResponse response = totpService.setupTotp(userId, username);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/enable")
  public ResponseEntity<TotpVerifyResponse> enableTotp(
    @Valid @RequestBody TotpEnableRequest request
  ) {
    TotpVerifyResponse response = totpService.enableTotp(request);
    if (!response.isValid()) {
      return ResponseEntity.badRequest().body(response);
    }
    return ResponseEntity.ok(response);
  }

  @PostMapping("/verify")
  public ResponseEntity<TotpVerifyResponse> verifyTotp(
    @Valid @RequestBody TotpVerifyRequest request
  ) {
    TotpVerifyResponse response = totpService.verifyTotp(request);
    if (!response.isValid()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    return ResponseEntity.ok(response);
  }

  @PostMapping("/disable/{userId}")
  public ResponseEntity<Map<String, String>> disableTotp(
    @PathVariable UUID userId,
    @RequestBody Map<String, String> body
  ) {
    String code = body.get("code");
    if (code == null || code.isBlank()) {
      return ResponseEntity.badRequest().body(
        Map.of("message", "Verification code is required to disable 2FA")
      );
    }
    totpService.disableTotp(userId, code);
    return ResponseEntity.ok(Map.of("message", "2FA has been successfully disabled"));
  }

  @GetMapping("/status/{userId}")
  public ResponseEntity<TotpStatusResponse> getStatus(@PathVariable UUID userId) {
    TotpStatusResponse response = totpService.getStatus(userId);
    return ResponseEntity.ok(response);
  }

  @GetMapping(value = "/qr/{userId}", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> getQrCodePng(@PathVariable UUID userId) {
    byte[] imageBytes = totpService.getQrCodePng(userId);
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"totp-qr.png\"")
      .body(imageBytes);
  }
}
