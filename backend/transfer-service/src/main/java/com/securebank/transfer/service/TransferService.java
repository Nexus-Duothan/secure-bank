package com.securebank.transfer.service;

import com.securebank.transfer.client.AccountSnapshot;
import com.securebank.transfer.client.AccountsClient;
import com.securebank.transfer.config.TransferServiceProperties;
import com.securebank.transfer.dto.TransferQuoteRequest;
import com.securebank.transfer.dto.TransferResponse;
import com.securebank.transfer.entity.Transfer;
import com.securebank.transfer.entity.TransferDailyUsage;
import com.securebank.transfer.enums.TransferStatus;
import com.securebank.transfer.messaging.TransferEventPublisher;
import com.securebank.transfer.repository.TransferDailyUsageRepository;
import com.securebank.transfer.repository.TransferRepository;
import com.securebank.transfer.security.CallerIdentity;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal Account-to-Account transfer orchestration (FR-14).
 *
 * <p>A transfer is always quoted before it executes (FR-17): {@link #quote} validates the request
 * and balance and stages a {@code PENDING_CONFIRMATION} row; {@link #confirm} re-validates under a
 * transaction and a locked daily-usage counter before moving money. Confirming an
 * already-{@code COMPLETED} transfer is a no-op that returns the original result, so a retried
 * confirm (client timeout, double click) can never execute twice (NFR-R2).
 */
@Service
@RequiredArgsConstructor
public class TransferService {

  // The platform has no multi-currency support yet; every account in the mocked accounts-service
  // data is LKR, so this is a placeholder until accounts-service exposes real per-account currency.
  private static final String DEFAULT_CURRENCY = "LKR";
  // Internal A2A transfers are fee-free and instant, matching what the frontend already displays.
  private static final BigDecimal FEE = BigDecimal.ZERO;

  private final TransferRepository transferRepository;
  private final TransferDailyUsageRepository dailyUsageRepository;
  private final AccountsClient accountsClient;
  private final TransferServiceProperties properties;
  private final TransferEventPublisher eventPublisher;

  @Transactional
  public TransferResponse quote(CallerIdentity caller, TransferQuoteRequest request, String idempotencyKey) {
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      var existing = transferRepository.findByInitiatedByUserIdAndIdempotencyKey(
        caller.userId(),
        idempotencyKey
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

    AccountSnapshot account = accountsClient.getAccount(request.fromAccountId());
    BigDecimal totalDebit = request.amount().add(FEE);
    if (account.balance().compareTo(totalDebit) < 0) {
      throw new InsufficientFundsException("Insufficient balance to cover this transfer");
    }
    assertWithinDailyLimit(request.fromAccountId(), request.amount(), peekDailyUsage(request.fromAccountId()));

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
        .idempotencyKey(idempotencyKey)
        .build()
    );
    return TransferResponse.from(transfer);
  }

  @Transactional
  public TransferResponse confirm(CallerIdentity caller, UUID transferId) {
    Transfer transfer = findOwned(caller, transferId);

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

    LocalDate today = LocalDate.now();
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

    transfer.setStatus(TransferStatus.COMPLETED);
    transfer.setConfirmedAt(Instant.now());
    Transfer saved = transferRepository.save(transfer);
    eventPublisher.publishCompleted(saved);
    return TransferResponse.from(saved);
  }

  /**
   * Single-shot compatibility path for the current frontend, which doesn't yet render a
   * confirmation screen between submit and execution. Quotes and immediately confirms in the same
   * transaction; once the UI grows a review step, callers should move to {@link #quote} +
   * {@link #confirm}.
   */
  @Transactional
  public TransferResponse quoteAndConfirm(
    CallerIdentity caller,
    TransferQuoteRequest request,
    String idempotencyKey
  ) {
    TransferResponse quoted = quote(caller, request, idempotencyKey);
    if (quoted.status() != TransferStatus.PENDING_CONFIRMATION) {
      return quoted;
    }
    return confirm(caller, quoted.id());
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
      .findById(new TransferDailyUsage.Key(accountId, LocalDate.now()))
      .map(TransferDailyUsage::getTotalAmount)
      .orElse(BigDecimal.ZERO);
  }

  private Transfer findOwned(CallerIdentity caller, UUID transferId) {
    return transferRepository
      .findByIdAndInitiatedByUserId(transferId, caller.userId())
      .orElseThrow(() -> new EntityNotFoundException("Transfer not found"));
  }
}
