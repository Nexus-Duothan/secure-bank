package com.securebank.transfer.service;

import com.securebank.transfer.client.AccountSnapshot;
import com.securebank.transfer.client.AccountsClient;
import com.securebank.transfer.client.LedgerEntry;
import com.securebank.transfer.client.TotpClient;
import com.securebank.transfer.config.TransferServiceProperties;
import com.securebank.transfer.dto.TransferQuoteRequest;
import com.securebank.transfer.dto.TransferResponse;
import com.securebank.transfer.entity.Transfer;
import com.securebank.transfer.entity.TransferDailyUsage;
import com.securebank.transfer.enums.TransferStatus;
import com.securebank.transfer.messaging.TransferEventPublisher;
import com.securebank.transfer.repository.PayeeRepository;
import com.securebank.transfer.repository.TransferDailyUsageRepository;
import com.securebank.transfer.repository.TransferRepository;
import com.securebank.transfer.security.CallerIdentity;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal Account-to-Account transfer orchestration.
 *
 * <p>A transfer is always quoted before it executes: {@link #quote} validates the request
 * and balance and stages a {@code PENDING_CONFIRMATION} row; {@link #confirm} locks that row and the
 * day's usage counter before moving money, so two concurrent confirms on the same transfer
 * serialise instead of both executing. Confirming an already-{@code COMPLETED} transfer is a no-op
 * that returns the original result, so a retried confirm (client timeout, double click) can never
 * execute twice.
 */
@Service
@RequiredArgsConstructor
public class TransferService {

  // The platform has no multi-currency support yet; every product accounts-service offers is in
  // LKR, so this is a placeholder until per-account currency is settled end to end.
  private static final String DEFAULT_CURRENCY = "LKR";
  // Internal A2A transfers are fee-free and instant, matching what the frontend already displays.
  private static final BigDecimal FEE = BigDecimal.ZERO;
  // Explicit zone so the daily-limit bucket boundary is Sri Lanka midnight regardless of the
  // JVM's default zone, matching ScheduledTransferExecutionService.SCHEDULE_ZONE.
  private static final ZoneId DAILY_USAGE_ZONE = ZoneId.of("Asia/Colombo");

  private final TransferRepository transferRepository;
  private final TransferDailyUsageRepository dailyUsageRepository;
  private final PayeeRepository payeeRepository;
  private final AccountsClient accountsClient;
  private final TransferServiceProperties properties;
  private final TransferEventPublisher eventPublisher;
  private final TotpClient totpClient;

  @Transactional
  public TransferResponse quote(
    CallerIdentity caller,
    TransferQuoteRequest request,
    String idempotencyKey
  ) {
    // Blank normalizes to null so it's never persisted: the unique index only excludes NULL,
    // not empty string, and a stored "" would collide across unrelated blank-header requests.
    String normalizedIdempotencyKey =
      idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
    if (normalizedIdempotencyKey != null) {
      var existing = transferRepository.findByInitiatedByUserIdAndIdempotencyKey(
        caller.userId(),
        normalizedIdempotencyKey
      );
      if (existing.isPresent()) {
        return TransferResponse.from(existing.get());
      }
    }

    if (request.toAccount().equalsIgnoreCase(request.fromAccountId())) {
      throw new IllegalArgumentException("Cannot transfer to the same account");
    }
    if (request.amount().compareTo(properties.limits().perTransaction()) > 0) {
      throw new LimitExceededException(
        "Amount exceeds the per-transaction limit of " + properties.limits().perTransaction()
      );
    }
    assertPayeeNotCoolingOff(caller.userId(), request.toAccount(), request.amount());

    AccountSnapshot account = accountsClient.getAccount(request.fromAccountId());
    BigDecimal totalDebit = request.amount().add(FEE);
    if (account.balance().compareTo(totalDebit) < 0) {
      throw new InsufficientFundsException("Insufficient balance to cover this transfer");
    }
    assertWithinDailyLimit(
      request.fromAccountId(),
      request.amount(),
      peekDailyUsage(request.fromAccountId())
    );

    Transfer transfer = transferRepository.save(
      Transfer.builder()
        .initiatedByUserId(caller.userId())
        .fromAccountId(request.fromAccountId())
        .toAccount(request.toAccount())
        .amount(request.amount())
        .fee(FEE)
        .currency(account.currency() == null ? DEFAULT_CURRENCY : account.currency())
        .note(request.note())
        .status(TransferStatus.PENDING_CONFIRMATION)
        .idempotencyKey(normalizedIdempotencyKey)
        .build()
    );
    return TransferResponse.from(transfer);
  }

  @Transactional
  public TransferResponse confirm(CallerIdentity caller, UUID transferId) {
    return confirmInternal(caller, transferId);
  }

  @Transactional
  public TransferResponse confirm(CallerIdentity caller, UUID transferId, String totpCode) {
    if (!totpClient.verify(caller.userId(), totpCode)) {
      throw new OtpVerificationException("Invalid authenticator code");
    }
    return confirmInternal(caller, transferId);
  }

  private TransferResponse confirmInternal(CallerIdentity caller, UUID transferId) {
    Transfer transfer = transferRepository
      .findForUpdateByIdAndInitiatedByUserId(transferId, caller.userId())
      .orElseThrow(() -> new EntityNotFoundException("Transfer not found"));

    if (transfer.getStatus() == TransferStatus.COMPLETED) {
      return TransferResponse.from(transfer);
    }
    if (transfer.getStatus() != TransferStatus.PENDING_CONFIRMATION) {
      throw new InvalidTransferStateException(
        "Transfer is " + transfer.getStatus() + " and can no longer be confirmed"
      );
    }

    AccountSnapshot account = accountsClient.getAccount(transfer.getFromAccountId());
    if (account.balance().compareTo(transfer.totalDebit()) < 0) {
      return fail(transfer, "Insufficient balance at confirmation time");
    }

    LocalDate today = LocalDate.now(DAILY_USAGE_ZONE);
    TransferDailyUsage usage = dailyUsageRepository
      .findForUpdate(transfer.getFromAccountId(), today)
      .orElseGet(() ->
        TransferDailyUsage.builder()
          .accountId(transfer.getFromAccountId())
          .usageDate(today)
          .totalAmount(BigDecimal.ZERO)
          .build()
      );
    BigDecimal projectedTotal = usage.getTotalAmount().add(transfer.getAmount());
    if (projectedTotal.compareTo(properties.limits().daily()) > 0) {
      return fail(transfer, "Daily transfer limit exceeded");
    }
    usage.setTotalAmount(projectedTotal);
    dailyUsageRepository.save(usage);

    postToLedger(transfer);

    transfer.setStatus(TransferStatus.COMPLETED);
    transfer.setConfirmedAt(Instant.now());
    Transfer saved = transferRepository.save(transfer);
    eventPublisher.publishCompleted(saved);
    return TransferResponse.from(saved);
  }

  /**
   * Moves the money in accounts-service, which owns the ledger. Both entries carry the transfer id
   * as their reference, so a confirm that is retried after a timeout re-posts nothing.
   */
  private void postToLedger(Transfer transfer) {
    String payeeName = payeeRepository
      .findByOwnerUserIdAndAccountReferenceIgnoreCase(
        transfer.getInitiatedByUserId(),
        transfer.getToAccount()
      )
      .map(payee -> payee.getNickname())
      .filter(nickname -> nickname != null && !nickname.isBlank())
      .orElse("account " + transfer.getToAccount());

    accountsClient.postDebit(
      transfer.getFromAccountId(),
      new LedgerEntry(
        transfer.totalDebit(),
        transfer.getCurrency(),
        "TRANSFER-OUT-" + transfer.getId(),
        "Transfer to " + payeeName,
        "Transfer",
        "TRANSFER",
        "SecureBank Transfer"
      )
    );

    accountsClient.postCreditByAccountNumber(
      transfer.getToAccount(),
      new LedgerEntry(
        transfer.getAmount(),
        transfer.getCurrency(),
        "TRANSFER-IN-" + transfer.getId(),
        "Transfer received",
        "Transfer",
        "TRANSFER",
        "SecureBank Transfer"
      )
    );
  }

  @Transactional(readOnly = true)
  public TransferResponse get(CallerIdentity caller, UUID transferId) {
    return TransferResponse.from(findOwned(caller, transferId));
  }

  private TransferResponse fail(Transfer transfer, String reason) {
    transfer.setStatus(TransferStatus.FAILED);
    transfer.setFailureReason(reason);
    Transfer saved = transferRepository.save(transfer);
    eventPublisher.publishFailed(saved, reason);
    return TransferResponse.from(saved);
  }

  private void assertPayeeNotCoolingOff(UUID ownerUserId, String toAccount, BigDecimal amount) {
    if (amount.compareTo(properties.limits().payeeCoolingOffThreshold()) < 0) {
      return;
    }
    payeeRepository
      .findByOwnerUserIdAndAccountReferenceIgnoreCase(ownerUserId, toAccount)
      .filter(payee -> payee.isCoolingOff(Instant.now()))
      .ifPresent(payee -> {
        throw new PayeeCoolingOffException(
          "This payee is still within its 12-hour cooling-off period for transfers of this size"
        );
      });
  }

  private void assertWithinDailyLimit(String accountId, BigDecimal amount, BigDecimal usedToday) {
    if (usedToday.add(amount).compareTo(properties.limits().daily()) > 0) {
      throw new LimitExceededException(
        "This transfer would exceed the daily transfer limit for account " + accountId
      );
    }
  }

  /** Non-locking peek used only to fail fast at quote time; {@link #confirm} re-checks under lock. */
  private BigDecimal peekDailyUsage(String accountId) {
    return dailyUsageRepository
      .findById(new TransferDailyUsage.Key(accountId, LocalDate.now(DAILY_USAGE_ZONE)))
      .map(TransferDailyUsage::getTotalAmount)
      .orElse(BigDecimal.ZERO);
  }

  private Transfer findOwned(CallerIdentity caller, UUID transferId) {
    return transferRepository
      .findByIdAndInitiatedByUserId(transferId, caller.userId())
      .orElseThrow(() -> new EntityNotFoundException("Transfer not found"));
  }
}
