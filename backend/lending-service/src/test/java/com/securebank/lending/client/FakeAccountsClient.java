package com.securebank.lending.client;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Stands in for accounts-service in tests, since it's still a bare scaffold and the test suite
 * shouldn't require a live instance. Defaults every account to a large balance; tests can
 * override a specific account's balance to exercise insufficient-funds / retry paths.
 */
@Component
@Primary
@Profile("test")
public class FakeAccountsClient implements AccountsClient {

  private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("1000000");
  private static final Map<String, BigDecimal> BALANCES = new ConcurrentHashMap<>();

  public static void reset() {
    BALANCES.clear();
  }

  public static void setBalance(String accountId, BigDecimal balance) {
    BALANCES.put(accountId, balance);
  }

  @Override
  public AccountSnapshot getAccount(String accountId) {
    return new AccountSnapshot(accountId, BALANCES.getOrDefault(accountId, DEFAULT_BALANCE), "LKR");
  }
}
