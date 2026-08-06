package com.securebank.transfer.client;

/**
 * Seam onto accounts-service, which owns the ledger: balances are read from it and completed
 * transfers are posted back to it, so nothing about a customer's money is held here.
 */
public interface AccountsClient {
  /**
   * @throws AccountNotFoundException if accounts-service has no such account
   * @throws AccountsUnavailableException if accounts-service can't be reached
   */
  AccountSnapshot getAccount(String accountId);

  /**
   * Takes the money off the sending account. The entry's reference makes the call idempotent, so
   * a retry after a timeout cannot debit the customer twice.
   *
   * @throws AccountNotFoundException if accounts-service has no such account
   * @throws AccountsUnavailableException if accounts-service can't be reached
   */
  void postDebit(String accountId, LedgerEntry entry);

  /**
   * Puts the money on the beneficiary's account when it is held at SecureBank.
   *
   * @return false when no SecureBank account holds that number (the money left the bank), so the
   *     caller can carry on rather than treating it as a failure
   * @throws AccountsUnavailableException if accounts-service can't be reached
   */
  boolean postCreditByAccountNumber(String accountNumber, LedgerEntry entry);
}
