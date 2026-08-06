package com.securebank.lending.client;

import java.math.BigDecimal;

/**
 * Seam onto accounts-service, which owns the ledger. Lending both reads balances and moves money
 * through it: an approved loan is credited to the customer's linked account, and each collected
 * installment is debited from it.
 */
public interface AccountsClient {
  /**
   * @throws AccountNotFoundException if accounts-service has no such account
   * @throws AccountsUnavailableException if accounts-service can't be reached
   */
  AccountSnapshot getAccount(String accountId);

  /**
   * Pays the loan principal into the customer's account.
   *
   * @throws AccountNotFoundException if accounts-service has no such account
   * @throws AccountsUnavailableException if accounts-service can't be reached
   */
  void credit(String accountId, BigDecimal amount, String currency, String reference, String label);

  /**
   * Takes an installment out of the customer's account.
   *
   * @throws InsufficientFundsException if the account cannot cover it (or is frozen)
   * @throws AccountNotFoundException if accounts-service has no such account
   * @throws AccountsUnavailableException if accounts-service can't be reached
   */
  void debit(String accountId, BigDecimal amount, String currency, String reference, String label);
}
