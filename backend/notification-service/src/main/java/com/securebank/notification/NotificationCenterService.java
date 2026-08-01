package com.securebank.notification;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class NotificationCenterService {

  private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern(
    "HH:mm"
  ).withZone(COLOMBO);
  private static final DateTimeFormatter GROUP_FORMAT = DateTimeFormatter.ofPattern(
    "dd MMM yyyy"
  ).withZone(COLOMBO);
  private static final DateTimeFormatter EMAIL_EXPIRY_FORMAT = DateTimeFormatter.ofPattern(
    "dd MMM yyyy HH:mm"
  ).withZone(COLOMBO);
  private static final List<AuditTransactionResponse> AUDIT_TRANSACTIONS = List.of(
    new AuditTransactionResponse(
      "txn-demo-001",
      "Ceylon Electricity Board",
      "Bill payment",
      "Kandy",
      new BigDecimal("-84.20"),
      "LKR",
      "2026-08-01T09:12:00",
      "Today - 01 Aug 2026",
      "J-94021",
      false
    ),
    new AuditTransactionResponse(
      "txn-demo-002",
      "Salary Deposit",
      "Income",
      "SecureBank Payroll",
      new BigDecimal("3200.00"),
      "LKR",
      "2026-07-31T08:45:00",
      "Yesterday - 31 Jul 2026",
      "J-94020",
      false
    ),
    new AuditTransactionResponse(
      "txn-demo-003",
      "Kumar's Grocers",
      "Card payment",
      "Kandy",
      new BigDecimal("-46.75"),
      "LKR",
      "2026-07-20T18:20:00",
      "20 Jul 2026",
      "J-93981",
      false
    ),
    new AuditTransactionResponse(
      "txn-demo-004",
      "Transfer to A. Silva",
      "Outgoing transfer",
      "SecureBank Transfer",
      new BigDecimal("-150.00"),
      "LKR",
      "2026-07-19T14:10:00",
      "19 Jul 2026",
      "J-93972",
      false
    ),
    new AuditTransactionResponse(
      "txn-demo-005",
      "Highway Fuel Stop",
      "Transport",
      "Kadawatha",
      new BigDecimal("-18.40"),
      "LKR",
      "2026-07-18T11:40:00",
      "18 Jul 2026",
      "J-93958",
      false
    ),
    new AuditTransactionResponse(
      "txn-demo-006",
      "Mobile Reload",
      "Bills",
      "Dialog",
      new BigDecimal("-12.00"),
      "LKR",
      "2026-07-17T20:05:00",
      "17 Jul 2026",
      "J-93940",
      false
    ),
    new AuditTransactionResponse(
      "txn-demo-007",
      "Rent Collection",
      "Income",
      "Standing order",
      new BigDecimal("950.00"),
      "LKR",
      "2026-07-15T07:15:00",
      "15 Jul 2026",
      "J-93901",
      false
    ),
    new AuditTransactionResponse(
      "txn-demo-008",
      "Pharmacy Care",
      "Health",
      "Kandy",
      new BigDecimal("-36.90"),
      "LKR",
      "2026-07-14T17:45:00",
      "14 Jul 2026",
      "J-93890",
      false
    ),
    new AuditTransactionResponse(
      "txn-demo-009",
      "Dialog Broadband",
      "Internet",
      "Online",
      new BigDecimal("-42.50"),
      "LKR",
      "2026-07-13T10:22:00",
      "13 Jul 2026",
      "J-93878",
      false
    ),
    new AuditTransactionResponse(
      "txn-demo-010",
      "Bookshop",
      "Education",
      "Peradeniya",
      new BigDecimal("-22.10"),
      "LKR",
      "2026-07-12T13:35:00",
      "12 Jul 2026",
      "J-93860",
      false
    )
  );

  private final NotificationProperties properties;
  private final CopyOnWriteArrayList<NotificationResponse> notifications =
    new CopyOnWriteArrayList<>();

  @Nullable
  private final JavaMailSender mailSender;

  private final RestClient restClient = RestClient.builder().build();

  public NotificationCenterService(
    NotificationProperties properties,
    @Nullable JavaMailSender mailSender
  ) {
    this.properties = properties;
    this.mailSender = mailSender;
    notifications.addAll(seedNotifications());
  }

  public List<NotificationResponse> getNotifications() {
    return List.copyOf(notifications);
  }

  public void markAllAsRead() {
    notifications.replaceAll(notification ->
      new NotificationResponse(
        notification.id(),
        notification.type(),
        notification.title(),
        notification.description(),
        notification.categoryLabel(),
        notification.timestamp(),
        notification.groupLabel(),
        true
      )
    );
  }

  public List<AuditTransactionResponse> getAuditTransactions() {
    return AUDIT_TRANSACTIONS;
  }

  public OtpChallengeDeliveryResponse queueOtpChallenge(OtpChallengeDeliveryRequest request) {
    if (hasText(request.phoneNumber())) {
      dispatchSms(request);
      return new OtpChallengeDeliveryResponse("queued", List.of("sms"), "");
    }

    return new OtpChallengeDeliveryResponse("not-queued", List.of(), "");
  }

  private void dispatchSms(OtpChallengeDeliveryRequest request) {
    String message =
      "SecureBank verification code: " +
      request.otpCode() +
      ". It expires at " +
      EMAIL_EXPIRY_FORMAT.format(request.expiresAt()) +
      ".";

    if (
      "twilio".equalsIgnoreCase(properties.sms().provider()) &&
      hasText(properties.sms().twilio().accountSid()) &&
      hasText(properties.sms().twilio().authToken()) &&
      hasText(properties.sms().twilio().fromNumber())
    ) {
      try {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", request.phoneNumber().trim());
        form.add("From", properties.sms().twilio().fromNumber().trim());
        form.add("Body", message);

        restClient
          .post()
          .uri(
            "https://api.twilio.com/2010-04-01/Accounts/{sid}/Messages.json",
            properties.sms().twilio().accountSid().trim()
          )
          .headers(headers ->
            headers.set(
              "Authorization",
              "Basic " +
                Base64.getEncoder().encodeToString(
                  (
                    properties.sms().twilio().accountSid().trim() +
                    ":" +
                    properties.sms().twilio().authToken().trim()
                  ).getBytes(java.nio.charset.StandardCharsets.UTF_8)
                )
            )
          )
          .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
          .body(form)
          .retrieve()
          .toBodilessEntity();

        log.info("SMS OTP sent via Twilio to {}", request.phoneNumber());
        return;
      } catch (RuntimeException exception) {
        log.warn(
          "Twilio SMS delivery failed for {}. Falling back to logged delivery. {}",
          request.phoneNumber(),
          exception.getMessage()
        );
      }
    }

    log.info(
      "SMS OTP queued for {} via {} sender {}. Code {} expires at {}.",
      request.phoneNumber(),
      properties.sms().provider(),
      properties.sms().sender(),
      request.otpCode(),
      EMAIL_EXPIRY_FORMAT.format(request.expiresAt())
    );
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private List<NotificationResponse> seedNotifications() {
    return List.of(
      new NotificationResponse(
        "ntf-demo-001",
        "security",
        "New sign-in verified",
        "Chrome on Windows from Colombo, LK was approved using your primary mobile device.",
        "Security alert",
        "14:22",
        "Today",
        false
      ),
      new NotificationResponse(
        "ntf-demo-002",
        "security",
        "Card transaction protected",
        "A card attempt of LKR 92,000.00 from an unfamiliar location was stopped for your safety.",
        "Security alert",
        "09:47",
        "Today",
        false
      ),
      new NotificationResponse(
        "ntf-demo-003",
        "info",
        "Loan installment received",
        "Your July loan installment of LKR 24,350.00 was received successfully.",
        null,
        "05 Jul - 08:00",
        "Earlier",
        true
      ),
      new NotificationResponse(
        "ntf-demo-004",
        "info",
        "Statement available",
        "Your June statement for Everyday Current is ready to download.",
        null,
        "01 Jul - 06:12",
        "Earlier",
        true
      )
    );
  }
}
