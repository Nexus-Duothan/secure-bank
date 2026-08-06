package com.securebank.lending.client;

/**
 * Seam onto accounts-service, which owns the ledger. Lending only reads balances through it today:
 * disbursements and installment collections are still recorded on the loan rather than posted to
 * the customer's account, so a balance check here is advisory rather than a reservation.
 */
public interface AccountsClient {
  /**
   * @throws AccountNotFoundException if accounts-service has no such account
   * @throws AccountsUnavailableException if accounts-service can't be reached
   */
  AccountSnapshot getAccount(String accountId);
}
