package com.securebank.notification.controller;

import com.securebank.notification.OtpChallengeDeliveryRequest;
import com.securebank.notification.OtpChallengeDeliveryResponse;
import com.securebank.notification.PasswordResetDeliveryRequest;
import com.securebank.notification.service.OtpChallengeDeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal relay used by accounts-service and user-service to get a one-time code
 * out to the customer.
 *
 * <p>Callers are other services on the internal network, not browsers, so they carry
 * no end-user JWT. The path is therefore permitted in {@code SecurityConfig} and must
 * never be exposed through the API Gateway.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class OtpChallengeController {

  private final OtpChallengeDeliveryService otpChallengeDeliveryService;

  @PostMapping("/otp-challenges")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public OtpChallengeDeliveryResponse queueOtpChallenge(
    @Valid @RequestBody OtpChallengeDeliveryRequest request
  ) {
    return otpChallengeDeliveryService.queueOtpChallenge(request);
  }

  @PostMapping("/password-reset-links")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public OtpChallengeDeliveryResponse queuePasswordResetLink(
    @Valid @RequestBody PasswordResetDeliveryRequest request
  ) {
    return otpChallengeDeliveryService.queuePasswordResetLink(request);
  }
}
