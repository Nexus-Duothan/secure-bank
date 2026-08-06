package com.securebank.payments;

import com.securebank.payments.dto.PaymentResponse;
import com.securebank.payments.service.PaymentService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Merchant portal endpoints (FR-15/FR-20): a read-only view of takings,
 * payments in, and settlement payouts. Refunds and disputes are handled in the
 * bank's merchant system, so there are no mutation endpoints here. The API
 * Gateway stamps X-User-Role from the verified JWT, so the role header is
 * trustworthy for calls that came through the gateway.
 */
@RestController
@RequestMapping("/api/v1/payments/merchant")
@RequiredArgsConstructor
public class MerchantPaymentsController {

  private final PaymentService paymentService;

  private static final List<MerchantPayment> PAYMENTS = List.of(
    new MerchantPayment(
      "mpay-1001",
      "K. Kapitiarachchi",
      "QR scan",
      "INV-2081",
      "LKR",
      new BigDecimal("46.75"),
      "SETTLED",
      "2026-08-01T10:42:00Z"
    ),
    new MerchantPayment(
      "mpay-1002",
      "A. Silva",
      "Online payment",
      "INV-2080",
      "LKR",
      new BigDecimal("1220.00"),
      "PENDING",
      "2026-08-01T09:58:00Z"
    ),
    new MerchantPayment(
      "mpay-1003",
      "S. Fernando",
      "QR scan",
      "INV-2079",
      "LKR",
      new BigDecimal("310.40"),
      "SETTLED",
      "2026-08-01T09:12:00Z"
    ),
    new MerchantPayment(
      "mpay-1004",
      "R. Jayasinghe",
      "Online payment",
      "INV-2075",
      "LKR",
      new BigDecimal("88.00"),
      "REFUNDED",
      "2026-07-31T15:30:00Z"
    )
  );

  @GetMapping("/summary")
  public MerchantSummary getSummary(
    @RequestHeader(value = "X-User-Role", required = false) String role
  ) {
    requireMerchant(role);
    return new MerchantSummary(
      "Kumar's Grocers",
      "LKR",
      new BigDecimal("18640.50"),
      23,
      1,
      new BigDecimal("64210.75"),
      "04 Aug 2026"
    );
  }

  @GetMapping("/payments")
  @PreAuthorize("hasRole('MERCHANT')")
  public List<PaymentResponse> getPayments(Authentication authentication) {
    return paymentService.getMerchantPayments(UUID.fromString(authentication.getName()));
  }

  @GetMapping("/settlements")
  public List<MerchantSettlement> getSettlements(
    @RequestHeader(value = "X-User-Role", required = false) String role
  ) {
    requireMerchant(role);
    return List.of(
      new MerchantSettlement(
        "stl-1001",
        "28 Jul – 03 Aug 2026",
        "LKR",
        new BigDecimal("64850.75"),
        new BigDecimal("640.00"),
        new BigDecimal("64210.75"),
        "SCHEDULED",
        "04 Aug 2026"
      ),
      new MerchantSettlement(
        "stl-1002",
        "21 Jul – 27 Jul 2026",
        "LKR",
        new BigDecimal("71204.10"),
        new BigDecimal("702.40"),
        new BigDecimal("70501.70"),
        "PAID",
        "28 Jul 2026"
      ),
      new MerchantSettlement(
        "stl-1003",
        "14 Jul – 20 Jul 2026",
        "LKR",
        new BigDecimal("58990.00"),
        new BigDecimal("583.20"),
        new BigDecimal("58406.80"),
        "PAID",
        "21 Jul 2026"
      )
    );
  }

  private void requireMerchant(String role) {
    if (!"MERCHANT".equals(role)) {
      throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "Only merchants may view merchant payment data"
      );
    }
  }

  public record MerchantSummary(
    String merchantName,
    String currency,
    BigDecimal todayTotal,
    int paymentsToday,
    int refundsToday,
    BigDecimal pendingSettlement,
    String nextPayoutDate
  ) {}

  public record MerchantPayment(
    String id,
    String payerName,
    String method,
    String reference,
    String currency,
    BigDecimal amount,
    String status,
    String timestamp
  ) {}

  public record MerchantSettlement(
    String id,
    String periodLabel,
    String currency,
    BigDecimal grossAmount,
    BigDecimal fees,
    BigDecimal netAmount,
    String status,
    String payoutDate
  ) {}
}
