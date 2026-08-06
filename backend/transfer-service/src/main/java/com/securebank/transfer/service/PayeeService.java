package com.securebank.transfer.service;

import com.securebank.transfer.client.TotpClient;
import com.securebank.transfer.config.TransferServiceProperties;
import com.securebank.transfer.dto.AddPayeeRequest;
import com.securebank.transfer.dto.ConfirmPayeeRequest;
import com.securebank.transfer.dto.EditPayeeRequest;
import com.securebank.transfer.dto.PayeeChallengeResponse;
import com.securebank.transfer.dto.PayeeResponse;
import com.securebank.transfer.entity.Payee;
import com.securebank.transfer.entity.PendingPayeeAddition;
import com.securebank.transfer.repository.PayeeRepository;
import com.securebank.transfer.repository.PendingPayeeAdditionRepository;
import com.securebank.transfer.security.CallerIdentity;
import com.securebank.transfer.service.notification.PayeeAlertService;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saved-payee management. Adding a payee is staged as a {@link PendingPayeeAddition} and
 * only takes effect once the caller confirms it with the current code from their authenticator app
 * (TOTP), so a hijacked session alone can't add a transfer destination. A confirmed payee carries a
 * 12-hour cooling-off window during which {@link TransferService} rejects large transfers to it.
 */
@Service
@RequiredArgsConstructor
public class PayeeService {

  private static final int COOLING_OFF_HOURS = 12;

  private final PayeeRepository payeeRepository;
  private final PendingPayeeAdditionRepository pendingPayeeAdditionRepository;
  private final TransferServiceProperties properties;
  private final PayeeAlertService payeeAlertService;
  private final TotpClient totpClient;

  @Transactional(readOnly = true)
  public List<PayeeResponse> listPayees(CallerIdentity caller) {
    return payeeRepository
      .findByOwnerUserIdOrderByCreatedAtDesc(caller.userId())
      .stream()
      .map(PayeeResponse::from)
      .toList();
  }

  /**
   * Stages the payee. Nothing is sent anywhere: the code comes from the authenticator app the
   * caller already enrolled, which also closes the old gap where a generated SMS code had no
   * delivery channel to reach the customer through.
   */
  @Transactional
  public PayeeChallengeResponse requestAddPayee(CallerIdentity caller, AddPayeeRequest request) {
    if (
      payeeRepository.existsByOwnerUserIdAndAccountReferenceIgnoreCase(
        caller.userId(),
        request.accountReference()
      )
    ) {
      throw new ConflictException("That account is already saved as a payee");
    }

    PendingPayeeAddition saved = pendingPayeeAdditionRepository.save(
      PendingPayeeAddition.builder()
        .ownerUserId(caller.userId())
        .nickname(request.nickname().trim())
        .accountReference(request.accountReference().trim())
        .expiresAt(Instant.now().plus(properties.otp().ttl()))
        .build()
    );

    return new PayeeChallengeResponse(
      saved.getId(),
      saved.getExpiresAt(),
      "Enter the current six digit code from your authenticator app to confirm this payee.",
      null
    );
  }

  /**
   * {@code noRollbackFor} keeps the incremented attempt counter durable on a wrong code; without
   * it the rollback would restore the counter to zero and leave the code brute forceable.
   */
  @Transactional(noRollbackFor = OtpVerificationException.class)
  public PayeeResponse confirmAddPayee(
    CallerIdentity caller,
    UUID changeRequestId,
    ConfirmPayeeRequest request
  ) {
    PendingPayeeAddition change = pendingPayeeAdditionRepository
      .findByIdAndOwnerUserId(changeRequestId, caller.userId())
      .orElseThrow(() -> new EntityNotFoundException("Payee change request not found"));

    verifyTotp(caller, change, request.otpCode());

    if (
      payeeRepository.existsByOwnerUserIdAndAccountReferenceIgnoreCase(
        caller.userId(),
        change.getAccountReference()
      )
    ) {
      throw new ConflictException("That account is already saved as a payee");
    }

    Payee payee = payeeRepository.save(
      Payee.builder()
        .ownerUserId(caller.userId())
        .nickname(change.getNickname())
        .accountReference(change.getAccountReference())
        .coolingOffUntil(Instant.now().plus(Duration.ofHours(COOLING_OFF_HOURS)))
        .build()
    );

    change.setConfirmed(true);
    change.setConfirmedAt(Instant.now());
    pendingPayeeAdditionRepository.save(change);

    payeeAlertService.sendPayeeAddedAlert(payee);
    return PayeeResponse.from(payee);
  }

  @Transactional
  public PayeeResponse editPayee(CallerIdentity caller, UUID payeeId, EditPayeeRequest request) {
    Payee payee = findOwned(caller, payeeId);
    payee.setNickname(request.nickname().trim());
    return PayeeResponse.from(payeeRepository.save(payee));
  }

  @Transactional
  public void removePayee(CallerIdentity caller, UUID payeeId) {
    Payee payee = findOwned(caller, payeeId);
    payeeRepository.delete(payee);
  }

  private void verifyTotp(
    CallerIdentity caller,
    PendingPayeeAddition change,
    String submittedCode
  ) {
    if (change.isConfirmed()) {
      throw new ConflictException("Payee addition already confirmed");
    }
    int maxAttempts = properties.otp().maxAttempts();
    if (change.getFailedAttempts() >= maxAttempts) {
      throw new OtpVerificationException(
        "Too many incorrect codes for this request; start the change again"
      );
    }
    if (change.isExpired(Instant.now())) {
      throw new OtpVerificationException("Verification request expired");
    }
    if (!totpClient.verify(caller.userId(), submittedCode)) {
      change.setFailedAttempts(change.getFailedAttempts() + 1);
      pendingPayeeAdditionRepository.save(change);
      int remaining = Math.max(0, maxAttempts - change.getFailedAttempts());
      throw new OtpVerificationException(
        "Invalid authenticator code, " + remaining + " attempt(s) remaining"
      );
    }
  }

  private Payee findOwned(CallerIdentity caller, UUID payeeId) {
    return payeeRepository
      .findByIdAndOwnerUserId(payeeId, caller.userId())
      .orElseThrow(() -> new EntityNotFoundException("Payee not found"));
  }
}
