package com.securebank.notification;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationCenterService notificationCenterService;

  @GetMapping("/api/v1/notifications/")
  public List<NotificationResponse> getNotifications() {
    return notificationCenterService.getNotifications();
  }

  @PostMapping("/api/v1/notifications/mark-all-read")
  public Map<String, String> markAllRead() {
    notificationCenterService.markAllAsRead();
    return Map.of("status", "ok");
  }

  @PostMapping("/api/v1/notifications/otp-challenges")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public OtpChallengeDeliveryResponse queueOtpChallenge(
    @Valid @RequestBody OtpChallengeDeliveryRequest request
  ) {
    return notificationCenterService.queueOtpChallenge(request);
  }

  @GetMapping("/api/v1/audit/transactions")
  public List<AuditTransactionResponse> getAuditTransactions() {
    return notificationCenterService.getAuditTransactions();
  }
}
