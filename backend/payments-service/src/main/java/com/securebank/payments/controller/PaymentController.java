package com.securebank.payments.controller;

import com.securebank.payments.dto.*;
import com.securebank.payments.enums.PaymentStatus;
import com.securebank.payments.service.MerchantService;
import com.securebank.payments.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;
  private final MerchantService merchantService;

  @PostMapping("/pay")
  public ResponseEntity<PaymentResponse> pay(
    Authentication authentication,
    @Valid @RequestBody PayRequest request
  ) {
    UUID payerUserId = UUID.fromString(authentication.getName());
    PaymentResponse response = paymentService.pay(payerUserId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/bills")
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<BillPaymentResponse> payBill(
    Authentication authentication,
    @Valid @RequestBody PayBillRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
      paymentService.payBill(UUID.fromString(authentication.getName()), request)
    );
  }

  @PostMapping("/qr/pay")
  public ResponseEntity<PaymentResponse> payByQr(
    Authentication authentication,
    @Valid @RequestBody QrPayRequest request
  ) {
    UUID payerUserId = UUID.fromString(authentication.getName());
    PaymentResponse response = paymentService.payByQr(payerUserId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<PaymentResponse> getPayment(
    Authentication authentication,
    @PathVariable UUID id
  ) {
    UUID requesterId = UUID.fromString(authentication.getName());
    PaymentResponse response = paymentService.getById(id, requesterId, isOfficer(authentication));
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}/receipt")
  public ResponseEntity<ReceiptResponse> getReceipt(
    Authentication authentication,
    @PathVariable UUID id
  ) {
    UUID requesterId = UUID.fromString(authentication.getName());
    ReceiptResponse response = paymentService.getReceipt(
      id,
      requesterId,
      isOfficer(authentication)
    );
    return ResponseEntity.ok(response);
  }

  private boolean isOfficer(Authentication authentication) {
    return authentication
      .getAuthorities()
      .stream()
      .anyMatch(
        a -> a.getAuthority().equals("ROLE_BANK_OFFICER") || a.getAuthority().equals("ROLE_ADMIN")
      );
  }

  @GetMapping
  public ResponseEntity<Page<PaymentResponse>> getHistory(
    Authentication authentication,
    @RequestParam(required = false) PaymentStatus status,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    UUID payerUserId = UUID.fromString(authentication.getName());
    Pageable pageable = PageRequest.of(page, size);
    Page<PaymentResponse> response = paymentService.getHistory(payerUserId, status, pageable);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/merchants/register")
  @PreAuthorize("hasRole('MERCHANT')")
  public ResponseEntity<MerchantResponse> registerMerchant(
    Authentication authentication,
    @Valid @RequestBody MerchantRegisterRequest request
  ) {
    UUID merchantUserId = UUID.fromString(authentication.getName());
    MerchantResponse response = merchantService.register(merchantUserId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/merchants/{code}")
  public ResponseEntity<MerchantResponse> getMerchant(@PathVariable String code) {
    MerchantResponse response = merchantService.getByCode(code);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/officer/held")
  @PreAuthorize("hasAnyRole('BANK_OFFICER', 'ADMIN')")
  public ResponseEntity<List<PaymentResponse>> getHeldForReview() {
    return ResponseEntity.ok(paymentService.getHeldForReview());
  }

  @PostMapping("/officer/{id}/review")
  @PreAuthorize("hasAnyRole('BANK_OFFICER', 'ADMIN')")
  public ResponseEntity<PaymentResponse> reviewPayment(
    Authentication authentication,
    @PathVariable UUID id,
    @Valid @RequestBody PaymentReviewRequest request
  ) {
    UUID officerId = UUID.fromString(authentication.getName());
    PaymentResponse response = paymentService.review(id, officerId, request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/merchant/payments/{id}/refund")
  @PreAuthorize("hasRole('MERCHANT')")
  public ResponseEntity<PaymentResponse> refundPayment(
    Authentication authentication,
    @PathVariable UUID id,
    @Valid @RequestBody RefundRequest request
  ) {
    return ResponseEntity.ok(
      paymentService.refund(UUID.fromString(authentication.getName()), id, request.totpCode())
    );
  }
}
