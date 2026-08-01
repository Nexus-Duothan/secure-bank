package com.securebank.lending.client;

/**
 * Seam onto accounts-service. It currently answers from hardcoded demo data (no real ledger,
 * and no debit/mutate endpoint of any kind yet), so balance checks here are advisory rather
 * than authoritative until that service grows a persistent ledger — the same limitation
 * transfer-service documents for its own AccountsClient. Once accounts-service exposes a real
 * debit contract, only the implementation behind this interface needs to change.
 */
public interface AccountsClient {
  /**
   * @throws AccountNotFoundException if accounts-service has no such account
   * @throws AccountsUnavailableException if accounts-service can't be reached
   */
  AccountSnapshot getAccount(String accountId);
}
