package com.securebank.transfer.client;

/**
 * Seam onto accounts-service. It currently answers from hardcoded demo data (no real ledger yet);
 * once accounts-service grows a persistent balance, only the implementation behind this interface
 * needs to change.
 */
public interface AccountsClient {
  /**
   * @throws AccountNotFoundException if accounts-service has no such account
   * @throws AccountsUnavailableException if accounts-service can't be reached
   */
  AccountSnapshot getAccount(String accountId);
}
