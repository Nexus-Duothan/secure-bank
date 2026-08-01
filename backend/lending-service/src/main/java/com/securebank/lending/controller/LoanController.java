package com.securebank.lending.controller;

import com.securebank.lending.dto.*;
import com.securebank.lending.service.LoanApplicationService;
import com.securebank.lending.service.LoanService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Loan origination, review, and repayment (FR-22 through FR-26). Every route acts on the
 * caller id resolved from the verified access token, never on a request-supplied user id.
 */
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

  private final LoanApplicationService loanApplicationService;
  private final LoanService loanService;

  @PostMapping("/apply")
  public ResponseEntity<LoanApplicationResponse> apply(
    Authentication authentication,
    @Valid @RequestBody LoanApplicationRequest request
  ) {
    UUID applicantUserId = callerId(authentication);
    return ResponseEntity.status(HttpStatus.CREATED).body(
      loanApplicationService.apply(applicantUserId, request)
    );
  }

  @GetMapping("/applications")
  public ResponseEntity<List<LoanApplicationResponse>> listApplications(
    Authentication authentication
  ) {
    return ResponseEntity.ok(loanApplicationService.list(callerId(authentication)));
  }

  @GetMapping("/applications/{id}")
  public ResponseEntity<LoanApplicationResponse> getApplication(
    Authentication authentication,
    @PathVariable UUID id
  ) {
    return ResponseEntity.ok(loanApplicationService.get(callerId(authentication), id));
  }

  @GetMapping("/officer/pending")
  @PreAuthorize("hasAnyRole('BANK_OFFICER', 'ADMIN')")
  public ResponseEntity<List<LoanApplicationResponse>> listPendingReview() {
    return ResponseEntity.ok(loanApplicationService.listPendingReview());
  }

  @PostMapping("/officer/{id}/review")
  @PreAuthorize("hasAnyRole('BANK_OFFICER', 'ADMIN')")
  public ResponseEntity<LoanApplicationResponse> reviewApplication(
    Authentication authentication,
    @PathVariable UUID id,
    @Valid @RequestBody LoanApplicationReviewRequest request
  ) {
    UUID officerId = callerId(authentication);
    return ResponseEntity.ok(loanApplicationService.review(officerId, id, request));
  }

  @GetMapping
  public ResponseEntity<List<LoanResponse>> listLoans(Authentication authentication) {
    return ResponseEntity.ok(loanService.list(callerId(authentication)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<LoanResponse> getLoan(
    Authentication authentication,
    @PathVariable UUID id
  ) {
    return ResponseEntity.ok(loanService.get(callerId(authentication), id));
  }

  @GetMapping("/{id}/installments")
  public ResponseEntity<List<LoanInstallmentResponse>> getInstallments(
    Authentication authentication,
    @PathVariable UUID id
  ) {
    return ResponseEntity.ok(loanService.listInstallments(callerId(authentication), id));
  }

  @PostMapping("/{id}/pay")
  public ResponseEntity<LoanResponse> payNow(Authentication authentication, @PathVariable UUID id) {
    return ResponseEntity.ok(loanService.payNow(callerId(authentication), id));
  }

  @PatchMapping("/{id}/autopay")
  public ResponseEntity<LoanResponse> updateAutopay(
    Authentication authentication,
    @PathVariable UUID id,
    @Valid @RequestBody AutoPayUpdateRequest request
  ) {
    return ResponseEntity.ok(
      loanService.updateAutopay(callerId(authentication), id, request.enabled())
    );
  }

  private UUID callerId(Authentication authentication) {
    return UUID.fromString(authentication.getName());
  }
}
