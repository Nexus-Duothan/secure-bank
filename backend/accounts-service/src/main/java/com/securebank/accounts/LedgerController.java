package com.securebank.accounts;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service to service ledger postings used by transfer, payments and lending.
 *
 * <p>These live under {@code /internal} on purpose: the API gateway only publishes
 * {@code /api/v1/accounts/**}, so a customer's browser can never reach them and move money on an
 * account directly.
 */
@RestController
@RequestMapping("/internal/v1/accounts")
@RequiredArgsConstructor
public class LedgerController {

  private final AccountsService accountsService;

  /** The bank-wide transaction journal, read by the admin audit view. */
  @GetMapping("/journal")
  public List<JournalEntryResponse> getJournal(@RequestParam(defaultValue = "200") int limit) {
    return accountsService.getJournal(limit);
  }

  /** Balance lookup for a service that has already checked who it is acting for. */
  @GetMapping("/{accountId}")
  public AccountSnapshotResponse getAccount(@PathVariable String accountId) {
    return accountsService.getAccountSnapshot(accountId);
  }

  @PostMapping("/{accountId}/debit")
  public LedgerEntryResponse debit(
    @PathVariable String accountId,
    @Valid @RequestBody LedgerEntryRequest request
  ) {
    return accountsService.debit(accountId, request);
  }

  @PostMapping("/{accountId}/credit")
  public LedgerEntryResponse credit(
    @PathVariable String accountId,
    @Valid @RequestBody LedgerEntryRequest request
  ) {
    return accountsService.credit(accountId, request);
  }

  /** Debits the customer's primary account, for callers that only know who is paying. */
  @PostMapping("/by-user/{userId}/debit")
  public LedgerEntryResponse debitByUser(
    @PathVariable String userId,
    @Valid @RequestBody LedgerEntryRequest request
  ) {
    return accountsService.debitPrimaryAccount(userId, request);
  }

  /** Debits a specific account only when it belongs to the authenticated user named by the caller. */
  @PostMapping("/by-user/{userId}/accounts/{accountId}/debit")
  public LedgerEntryResponse debitOwnedAccount(
    @PathVariable String userId,
    @PathVariable String accountId,
    @Valid @RequestBody LedgerEntryRequest request
  ) {
    return accountsService.debitOwnedAccount(userId, accountId, request);
  }

  /** Atomically returns merchant funds to a customer's primary account. */
  @PostMapping("/refund")
  public RefundLedgerResponse refund(@Valid @RequestBody RefundLedgerRequest request) {
    return accountsService.refund(request);
  }

  /** Credits a beneficiary the caller only knows by account number (in-bank transfers). */
  @PostMapping("/by-number/{accountNumber}/credit")
  public LedgerEntryResponse creditByAccountNumber(
    @PathVariable String accountNumber,
    @Valid @RequestBody LedgerEntryRequest request
  ) {
    return accountsService.creditByAccountNumber(accountNumber, request);
  }
}
