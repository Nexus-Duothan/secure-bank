package com.securebank.lending.client;

import com.securebank.lending.exception.InsufficientFundsException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Stands in for accounts-service in tests, so the suite does not need a live instance. It keeps a
 * real running balance per account rather than only answering questions: a disbursement raises it
 * and a collection lowers it, which is what lets a test assert the customer actually received
 * their loan. Defaults every account to a large balance; tests can set one low to exercise the
 * insufficient-funds and retry paths.
 */
@Component
@Primary
@Profile("test")
public class FakeAccountsClient implements AccountsClient {

  private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("1000000");
  private static final Map<String, BigDecimal> BALANCES = new ConcurrentHashMap<>();
  /** Mirrors the real ledger's idempotency: the same reference posted twice is a no-op. */
  private static final Set<String> POSTED_REFERENCES = ConcurrentHashMap.newKeySet();

  public static void reset() {
    BALANCES.clear();
    POSTED_REFERENCES.clear();
  }

  public static void setBalance(String accountId, BigDecimal balance) {
    BALANCES.put(accountId, balance);
  }

  public static BigDecimal balanceOf(String accountId) {
    return BALANCES.getOrDefault(accountId, DEFAULT_BALANCE);
  }

  @Override
  public AccountSnapshot getAccount(String accountId) {
    return new AccountSnapshot(accountId, balanceOf(accountId), "LKR");
  }

  @Override
  public void credit(
    String accountId,
    BigDecimal amount,
    String currency,
    String reference,
    String label
  ) {
    if (!POSTED_REFERENCES.add(reference)) {
      return;
    }
    BALANCES.put(accountId, balanceOf(accountId).add(amount));
  }

  @Override
  public void debit(
    String accountId,
    BigDecimal amount,
    String currency,
    String reference,
    String label
  ) {
    if (POSTED_REFERENCES.contains(reference)) {
      return;
    }
    BigDecimal balance = balanceOf(accountId);
    if (balance.compareTo(amount) < 0) {
      throw new InsufficientFundsException("Account " + accountId + " cannot cover this debit");
    }
    POSTED_REFERENCES.add(reference);
    BALANCES.put(accountId, balance.subtract(amount));
  }
}
