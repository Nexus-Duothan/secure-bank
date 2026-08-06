package com.securebank.payments.client;

import com.securebank.payments.client.dto.AccountDebitRequest;
import com.securebank.payments.client.dto.AccountDebitResponse;
import com.securebank.payments.client.dto.AccountRefundRequest;
import java.math.BigDecimal;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Stands in for accounts-service in tests, since it doesn't exist yet and the test suite
 * shouldn't require a live instance. Always succeeds; overrides the parent's debit() so
 * PaymentControllerTest can exercise the full flow without network calls.
 */
@Component
@Primary
@Profile("test")
public class FakeAccountsServiceClient extends AccountsServiceClient {

  public FakeAccountsServiceClient() {
    super(WebClient.builder(), "http://localhost:8084");
  }

  @Override
  public AccountDebitResponse debit(String accountId, AccountDebitRequest request) {
    return AccountDebitResponse.builder()
      .accountId(accountId)
      .newBalance(BigDecimal.valueOf(1_000_000))
      .build();
  }

  @Override
  public AccountDebitResponse debitAccount(
    String payerUserId,
    String accountId,
    AccountDebitRequest request
  ) {
    return AccountDebitResponse.builder()
      .accountId(accountId)
      .newBalance(BigDecimal.valueOf(1_000_000))
      .build();
  }

  @Override
  public void refund(AccountRefundRequest request) {
    // The accounts-service integration has its own atomic refund tests.
  }
}
