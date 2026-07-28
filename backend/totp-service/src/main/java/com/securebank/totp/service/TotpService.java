package com.securebank.totp.service;

import com.securebank.totp.dto.*;
import com.securebank.totp.entity.UserTotpSecret;
import com.securebank.totp.repository.UserTotpSecretRepository;
import com.securebank.totp.util.TotpEngine;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TotpService {

  private final UserTotpSecretRepository totpRepository;
  private final TotpEngine totpEngine;

  @Value("${totp.issuer:SecureBank}")
  private String issuer;

  @Transactional
  public TotpSetupResponse setupTotp(UUID userId, String username) {
    String secretKey = totpEngine.generateSecretKey();
    List<String> scratchCodes = totpEngine.generateScratchCodes(8);

    UserTotpSecret totpSecret = totpRepository
      .findByUserId(userId)
      .orElse(UserTotpSecret.builder().userId(userId).build());

    totpSecret.setSecretKey(secretKey);
    totpSecret.setEnabled(false);
    totpSecret.setScratchCodes(scratchCodes);
    totpRepository.save(totpSecret);

    String accountName = username != null ? username : userId.toString().substring(0, 8);
    String otpauthUrl = totpEngine.generateOtpauthUrl(secretKey, accountName, issuer);
    String qrCodeBase64 = totpEngine.generateQrCodeBase64(otpauthUrl, 250, 250);

    return TotpSetupResponse.builder()
      .userId(userId)
      .secretKey(secretKey)
      .otpauthUrl(otpauthUrl)
      .qrCodeBase64(qrCodeBase64)
      .scratchCodes(scratchCodes)
      .build();
  }

  @Transactional
  public TotpVerifyResponse enableTotp(TotpEnableRequest request) {
    UserTotpSecret totpSecret = totpRepository
      .findByUserId(request.getUserId())
      .orElseThrow(() -> new IllegalArgumentException("TOTP setup not initialized for user"));

    boolean valid = totpEngine.verifyTotp(totpSecret.getSecretKey(), request.getTotpCode(), 1);
    if (!valid) {
      return TotpVerifyResponse.builder()
        .valid(false)
        .message("Invalid TOTP code. Unable to enable 2FA.")
        .usedScratchCode(false)
        .build();
    }

    totpSecret.setEnabled(true);
    totpRepository.save(totpSecret);

    return TotpVerifyResponse.builder()
      .valid(true)
      .message("2FA has been successfully enabled.")
      .usedScratchCode(false)
      .build();
  }

  @Transactional
  public TotpVerifyResponse verifyTotp(TotpVerifyRequest request) {
    UserTotpSecret totpSecret = totpRepository.findByUserId(request.getUserId()).orElse(null);

    if (totpSecret == null || !totpSecret.isEnabled()) {
      return TotpVerifyResponse.builder()
        .valid(false)
        .message("MFA is not enabled for user")
        .usedScratchCode(false)
        .build();
    }

    String inputCode = request.getCode().trim();

    // 1. Verify 6-digit TOTP code
    if (inputCode.length() == 6 && totpEngine.verifyTotp(totpSecret.getSecretKey(), inputCode, 1)) {
      return TotpVerifyResponse.builder()
        .valid(true)
        .message("TOTP verification successful")
        .usedScratchCode(false)
        .build();
    }

    // 2. Check 8-character scratch recovery codes
    if (inputCode.length() == 8 && totpSecret.getScratchCodes().contains(inputCode)) {
      totpSecret.getScratchCodes().remove(inputCode);
      totpRepository.save(totpSecret);

      return TotpVerifyResponse.builder()
        .valid(true)
        .message("Emergency scratch code verified and consumed successfully")
        .usedScratchCode(true)
        .build();
    }

    return TotpVerifyResponse.builder()
      .valid(false)
      .message("Invalid TOTP or scratch code")
      .usedScratchCode(false)
      .build();
  }

  @Transactional
  public void disableTotp(UUID userId, String code) {
    UserTotpSecret totpSecret = totpRepository
      .findByUserId(userId)
      .orElseThrow(() -> new IllegalArgumentException("User does not have TOTP enabled"));

    TotpVerifyResponse verifyResult = verifyTotp(
      TotpVerifyRequest.builder().userId(userId).code(code).build()
    );

    if (!verifyResult.isValid()) {
      throw new IllegalArgumentException("Invalid verification code. Cannot disable 2FA.");
    }

    totpRepository.delete(totpSecret);
  }

  @Transactional(readOnly = true)
  public TotpStatusResponse getStatus(UUID userId) {
    UserTotpSecret totpSecret = totpRepository.findByUserId(userId).orElse(null);
    boolean enabled = totpSecret != null && totpSecret.isEnabled();
    boolean setupInitiated = totpSecret != null;

    return TotpStatusResponse.builder()
      .userId(userId)
      .enabled(enabled)
      .setupInitiated(setupInitiated)
      .build();
  }

  @Transactional(readOnly = true)
  public byte[] getQrCodePng(UUID userId) {
    UserTotpSecret totpSecret = totpRepository
      .findByUserId(userId)
      .orElseThrow(() -> new IllegalArgumentException("TOTP setup not initialized"));

    String otpauthUrl = totpEngine.generateOtpauthUrl(
      totpSecret.getSecretKey(),
      userId.toString().substring(0, 8),
      issuer
    );
    return totpEngine.generateQrCodePng(otpauthUrl, 300, 300);
  }
}
