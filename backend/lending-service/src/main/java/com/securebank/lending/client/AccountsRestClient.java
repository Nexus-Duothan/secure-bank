package com.securebank.lending.client;

import com.securebank.lending.config.LendingServiceProperties;
import com.securebank.lending.exception.InsufficientFundsException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AccountsRestClient implements AccountsClient {

  private final RestClient restClient;

  public AccountsRestClient(LendingServiceProperties properties) {
    this.restClient = RestClient.builder().baseUrl(properties.accountsClient().baseUrl()).build();
  }

  @Override
  public AccountSnapshot getAccount(String accountId) {
    try {
      AccountSnapshot snapshot = restClient
        .get()
        .uri("/internal/v1/accounts/{id}", accountId)
        .retrieve()
        .body(AccountSnapshot.class);
      if (snapshot == null) {
        throw new AccountsUnavailableException("accounts-service returned an empty response");
      }
      return snapshot;
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().value() == 404) {
        throw new AccountNotFoundException("Account " + accountId + " was not found");
      }
      throw new AccountsUnavailableException(
        "accounts-service responded with " + exception.getStatusCode(),
        exception
      );
    } catch (RestClientException exception) {
      throw new AccountsUnavailableException("Unable to reach accounts-service", exception);
    }
  }

  @Override
  public void credit(
    String accountId,
    BigDecimal amount,
    String currency,
    String reference,
    String label
  ) {
    post(accountId, "credit", amount, currency, reference, label, "LOAN_DISBURSEMENT");
  }

  @Override
  public void debit(
    String accountId,
    BigDecimal amount,
    String currency,
    String reference,
    String label
  ) {
    post(accountId, "debit", amount, currency, reference, label, "LOAN_REPAYMENT");
  }

  private void post(
    String accountId,
    String movement,
    BigDecimal amount,
    String currency,
    String reference,
    String label,
    String transactionType
  ) {
    LedgerEntryRequest body = new LedgerEntryRequest(
      amount,
      currency,
      reference,
      label,
      "Loans",
      transactionType,
      "SecureBank Lending"
    );
    try {
      restClient
        .post()
        .uri("/internal/v1/accounts/{id}/{movement}", accountId, movement)
        .body(body)
        .retrieve()
        .toBodilessEntity();
    } catch (RestClientResponseException exception) {
      int status = exception.getStatusCode().value();
      if (status == 404) {
        throw new AccountNotFoundException("Account " + accountId + " was not found");
      }
      // accounts-service answers 409 for both "not enough money" and "the account is frozen";
      // either way the movement cannot be posted right now.
      if (status == 409) {
        throw new InsufficientFundsException(
          "Account " + accountId + " cannot cover this " + movement
        );
      }
      throw new AccountsUnavailableException(
        "accounts-service responded with " + exception.getStatusCode(),
        exception
      );
    } catch (RestClientException exception) {
      throw new AccountsUnavailableException("Unable to reach accounts-service", exception);
    }
  }
}
