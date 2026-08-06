package com.securebank.payments.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.payments.client.AccountsServiceClient;
import com.securebank.payments.client.AuditRecoveryClient;
import com.securebank.payments.client.TotpClient;
import com.securebank.payments.client.dto.AccountDebitRequest;
import com.securebank.payments.client.dto.AccountRefundRequest;
import com.securebank.payments.client.dto.AnomalyReport;
import com.securebank.payments.dto.*;
import com.securebank.payments.entity.BillPayment;
import com.securebank.payments.entity.Merchant;
import com.securebank.payments.entity.VendorPayment;
import com.securebank.payments.enums.PaymentChannel;
import com.securebank.payments.enums.PaymentStatus;
import com.securebank.payments.exception.ResourceNotFoundException;
import com.securebank.payments.kafka.PaymentEventProducer;
import com.securebank.payments.kafka.event.PaymentCompletedEvent;
import com.securebank.payments.kafka.event.PaymentHeldEvent;
import com.securebank.payments.repository.BillPaymentRepository;
import com.securebank.payments.repository.VendorPaymentRepository;
import com.securebank.payments.util.QrCodeDecoder;
import com.securebank.payments.util.ReceiptReferenceGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

  /** security/audit-recovery-service's AnomalyEngine flags high-velocity activity at risk_score 75. */
  private static final int HIGH_VELOCITY_RISK_THRESHOLD = 75;

  private final TotpClient totpClient;

  private final VendorPaymentRepository vendorPaymentRepository;
  private final BillPaymentRepository billPaymentRepository;
  private final MerchantService merchantService;
  private final AccountsServiceClient accountsServiceClient;
  private final AuditRecoveryClient auditRecoveryClient;
  private final PaymentEventProducer paymentEventProducer;
  private final QrCodeDecoder qrCodeDecoder;
  private final ReceiptReferenceGenerator receiptReferenceGenerator;
  private final ObjectMapper objectMapper;

  public PaymentResponse pay(UUID payerUserId, PayRequest request) {
    Merchant merchant = merchantService.findActiveByCode(request.getMerchantCode());
    return processPayment(
      payerUserId,
      merchant,
      request.getAmount(),
      request.getCurrency(),
      PaymentChannel.DIRECT,
      request.getNote()
    );
  }

  public BillPaymentResponse payBill(UUID payerUserId, PayBillRequest request) {
    if (!totpClient.verify(payerUserId, request.totpCode())) {
      throw new IllegalArgumentException("Invalid authenticator code");
    }

    BillPayment payment = billPaymentRepository.save(
      BillPayment.builder()
        .payerUserId(payerUserId)
        .fromAccountId(request.fromAccountId())
        .billerCategory(request.billerCategory().trim())
        .billerName(request.billerName().trim())
        .referenceNumber(request.referenceNumber().trim())
        .amount(request.amount())
        .currency("LKR")
        .status("PENDING")
        .build()
    );

    accountsServiceClient.debitAccount(
      payerUserId.toString(),
      request.fromAccountId(),
      AccountDebitRequest.builder()
        .amount(request.amount())
        .currency("LKR")
        .reference("BILL-" + payment.getId())
        .merchant(request.billerName().trim())
        .category(request.billerCategory().trim())
        .transactionType("BILL_PAYMENT")
        .location("SecureBank Bill Pay")
        .build()
    );

    payment.setStatus("COMPLETED");
    payment.setCompletedAt(Instant.now());
    return BillPaymentResponse.from(billPaymentRepository.save(payment));
  }

  public PaymentResponse payByQr(UUID payerUserId, QrPayRequest request) {
    QrPaymentDetails details = qrCodeDecoder.decode(request.getQrPayload());
    Merchant merchant = merchantService.findActiveByCode(details.getMerchantCode());

    BigDecimal amount =
      request.getAmount() != null ? request.getAmount() : details.getSuggestedAmount();
    if (amount == null) {
      throw new IllegalArgumentException(
        "Payment amount was not provided by the QR code or the request"
      );
    }
    String currency = details.getCurrency() != null ? details.getCurrency() : "LKR";

    return processPayment(payerUserId, merchant, amount, currency, PaymentChannel.QR, null);
  }

  // Deliberately not @Transactional: accountsServiceClient.debit() and the audit-recovery
  // calls in runFraudCheck() are outbound WebClient I/O and must not hold a DB connection
  // for their duration. Each repository save() below is transactional on its own (Spring
  // Data JPA wraps repository methods individually), so this still leaves no partially
  // written row visible mid-write — it just stops bracketing network calls in one transaction.
  private PaymentResponse processPayment(
    UUID payerUserId,
    Merchant merchant,
    BigDecimal amount,
    String currency,
    PaymentChannel channel,
    String note
  ) {
    VendorPayment payment = vendorPaymentRepository.save(
      VendorPayment.builder()
        .payerUserId(payerUserId)
        .merchant(merchant)
        .amount(amount)
        .currency(currency)
        .channel(channel)
        .status(PaymentStatus.PENDING)
        .note(note)
        .build()
    );

    // Debit is load-bearing: the money moves in accounts-service, which owns the ledger. If that
    // call fails we deliberately let the exception propagate — GlobalExceptionHandler maps it to
    // 503 — rather than mark the payment COMPLETED without the money having actually moved.
    accountsServiceClient.debit(
      payerUserId.toString(),
      AccountDebitRequest.builder()
        .amount(amount)
        .currency(currency)
        .reference(payment.getId().toString())
        .merchant(merchant.getBusinessName())
        .category(merchant.getCategory() == null ? "Payments" : merchant.getCategory())
        .transactionType("CARD_PAYMENT")
        .location(channel == PaymentChannel.QR ? "QR payment" : "Vendor payment")
        .build()
    );

    Instant now = Instant.now();
    payment.setStatus(PaymentStatus.COMPLETED);
    payment.setReferenceNumber(receiptReferenceGenerator.generate(payment.getId(), now));
    payment = vendorPaymentRepository.save(payment);

    runFraudCheck(payment);

    paymentEventProducer.publishCompleted(
      PaymentCompletedEvent.builder()
        .paymentId(payment.getId())
        .payerUserId(payerUserId)
        .merchantId(merchant.getId())
        .amount(amount)
        .currency(currency)
        .referenceNumber(payment.getReferenceNumber())
        .occurredAt(now)
        .build()
    );

    return toResponse(payment);
  }

  /**
   * After a vendor payment, record it with audit-recovery-service and check
   * whether the payer now trips the high-velocity anomaly threshold. If so, the payment
   * (already COMPLETED against accounts-service) is flipped to HELD_FOR_REVIEW for an
   * officer to resolve via the /officer/{id}/review endpoint. There is no reversal call
   * to accounts-service here — that API doesn't exist yet either; this is a known
   * limitation documented in README.md.
   */
  private void runFraudCheck(VendorPayment payment) {
    JsonNode payload = objectMapper.valueToTree(
      Map.of(
        "paymentId",
        payment.getId().toString(),
        "merchantId",
        payment.getMerchant().getId().toString(),
        "amount",
        payment.getAmount(),
        "channel",
        payment.getChannel().name()
      )
    );

    auditRecoveryClient.recordEntry("VENDOR_PAYMENT", payment.getPayerUserId().toString(), payload);

    List<AnomalyReport> anomalies = auditRecoveryClient.getAnomaliesForUser(
      payment.getPayerUserId().toString()
    );
    boolean flagged = anomalies
      .stream()
      .anyMatch(a -> a.getRiskScore() >= HIGH_VELOCITY_RISK_THRESHOLD);

    if (flagged) {
      payment.setStatus(PaymentStatus.HELD_FOR_REVIEW);
      vendorPaymentRepository.save(payment);

      paymentEventProducer.publishHeld(
        PaymentHeldEvent.builder()
          .paymentId(payment.getId())
          .payerUserId(payment.getPayerUserId())
          .merchantId(payment.getMerchant().getId())
          .amount(payment.getAmount())
          .reason("High-velocity vendor payment activity detected")
          .occurredAt(Instant.now())
          .build()
      );
    }
  }

  @Transactional(readOnly = true)
  public PaymentResponse getById(UUID paymentId, UUID requesterUserId, boolean isOfficer) {
    VendorPayment payment = findOwnedOrVisible(paymentId, requesterUserId, isOfficer);
    return toResponse(payment);
  }

  @Transactional(readOnly = true)
  public ReceiptResponse getReceipt(UUID paymentId, UUID requesterUserId, boolean isOfficer) {
    VendorPayment payment = findOwnedOrVisible(paymentId, requesterUserId, isOfficer);
    if (
      payment.getStatus() != PaymentStatus.COMPLETED &&
      payment.getStatus() != PaymentStatus.HELD_FOR_REVIEW
    ) {
      throw new IllegalStateException("Receipt is not available until the payment has completed");
    }

    return ReceiptResponse.builder()
      .paymentId(payment.getId())
      .referenceNumber(payment.getReferenceNumber())
      .merchantName(payment.getMerchant().getBusinessName())
      .amount(payment.getAmount())
      .currency(payment.getCurrency())
      .issuedAt(payment.getUpdatedAt())
      .build();
  }

  @Transactional(readOnly = true)
  public Page<PaymentResponse> getHistory(
    UUID payerUserId,
    PaymentStatus status,
    Pageable pageable
  ) {
    Page<VendorPayment> page =
      status != null
        ? vendorPaymentRepository.findByPayerUserIdAndStatus(payerUserId, status, pageable)
        : vendorPaymentRepository.findByPayerUserId(payerUserId, pageable);
    return page.map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public List<PaymentResponse> getHeldForReview() {
    return vendorPaymentRepository
      .findByStatus(PaymentStatus.HELD_FOR_REVIEW)
      .stream()
      .map(this::toResponse)
      .toList();
  }

  @Transactional(readOnly = true)
  public List<PaymentResponse> getMerchantPayments(UUID merchantUserId) {
    return vendorPaymentRepository
      .findByMerchantMerchantUserIdOrderByCreatedAtDesc(merchantUserId)
      .stream()
      .map(this::toResponse)
      .toList();
  }

  @Transactional
  public PaymentResponse refund(UUID merchantUserId, UUID paymentId, String totpCode) {
    if (!totpClient.verify(merchantUserId, totpCode)) {
      throw new IllegalArgumentException("Invalid authenticator code");
    }
    VendorPayment payment = vendorPaymentRepository
      .findById(paymentId)
      .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
    if (!merchantUserId.equals(payment.getMerchant().getMerchantUserId())) {
      throw new ResourceNotFoundException("Payment not found: " + paymentId);
    }
    if (payment.getStatus() != PaymentStatus.COMPLETED) {
      throw new IllegalStateException("Only completed payments can be refunded");
    }

    accountsServiceClient.refund(
      new AccountRefundRequest(
        payment.getMerchant().getSettlementAccountId(),
        payment.getPayerUserId().toString(),
        payment.getAmount(),
        payment.getCurrency(),
        "refund:" + payment.getId(),
        payment.getMerchant().getBusinessName()
      )
    );
    payment.setStatus(PaymentStatus.REFUNDED);
    payment.setNote("Refunded by merchant after TOTP verification");
    return toResponse(vendorPaymentRepository.save(payment));
  }

  @Transactional
  public PaymentResponse review(UUID paymentId, UUID officerId, PaymentReviewRequest request) {
    if (!totpClient.verify(officerId, request.getTotpCode())) {
      throw new IllegalArgumentException("Invalid authenticator code");
    }
    VendorPayment payment = vendorPaymentRepository
      .findById(paymentId)
      .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

    if (payment.getStatus() != PaymentStatus.HELD_FOR_REVIEW) {
      throw new IllegalStateException("Only payments held for review can be reviewed");
    }

    boolean approve = Boolean.TRUE.equals(request.getApprove());
    payment.setStatus(approve ? PaymentStatus.COMPLETED : PaymentStatus.DECLINED);
    payment.setReviewedBy(officerId);
    payment.setReviewedAt(Instant.now());
    if (!approve) {
      payment.setFailureReason(
        request.getNote() != null ? request.getNote() : "Declined by reviewing officer"
      );
    }

    return toResponse(vendorPaymentRepository.save(payment));
  }

  private VendorPayment findOwnedOrVisible(
    UUID paymentId,
    UUID requesterUserId,
    boolean isOfficer
  ) {
    VendorPayment payment = vendorPaymentRepository
      .findById(paymentId)
      .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
    if (!isOfficer && !payment.getPayerUserId().equals(requesterUserId)) {
      throw new ResourceNotFoundException("Payment not found: " + paymentId);
    }
    return payment;
  }

  private PaymentResponse toResponse(VendorPayment payment) {
    return PaymentResponse.builder()
      .id(payment.getId())
      .payerUserId(payment.getPayerUserId())
      .merchantCode(payment.getMerchant().getMerchantCode())
      .merchantName(payment.getMerchant().getBusinessName())
      .amount(payment.getAmount())
      .currency(payment.getCurrency())
      .channel(payment.getChannel())
      .status(payment.getStatus())
      .note(payment.getNote())
      .referenceNumber(payment.getReferenceNumber())
      .failureReason(payment.getFailureReason())
      .createdAt(payment.getCreatedAt())
      .updatedAt(payment.getUpdatedAt())
      .build();
  }
}
