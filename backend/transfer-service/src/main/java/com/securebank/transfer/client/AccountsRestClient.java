package com.securebank.transfer.client;

import com.securebank.transfer.config.TransferServiceProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Talks to accounts-service over its {@code /internal} routes, which are not published by the API
 * gateway and are not scoped to a signed-in customer - ownership has already been checked here.
 */
@Component
public class AccountsRestClient implements AccountsClient {

  private final RestClient restClient;

  public AccountsRestClient(TransferServiceProperties properties) {
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
  public void postDebit(String accountId, LedgerEntry entry) {
    try {
      restClient
        .post()
        .uri("/internal/v1/accounts/{id}/debit", accountId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(entry)
        .retrieve()
        .toBodilessEntity();
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().value() == 404) {
        throw new AccountNotFoundException("Account " + accountId + " was not found");
      }
      throw new AccountsUnavailableException(
        "accounts-service rejected the debit with " + exception.getStatusCode(),
        exception
      );
    } catch (RestClientException exception) {
      throw new AccountsUnavailableException("Unable to reach accounts-service", exception);
    }
  }

  @Override
  public boolean postCreditByAccountNumber(String accountNumber, LedgerEntry entry) {
    try {
      restClient
        .post()
        .uri("/internal/v1/accounts/by-number/{accountNumber}/credit", accountNumber)
        .contentType(MediaType.APPLICATION_JSON)
        .body(entry)
        .retrieve()
        .toBodilessEntity();
      return true;
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().value() == 404) {
        // The beneficiary banks elsewhere, so only the sending side gets a ledger entry.
        return false;
      }
      throw new AccountsUnavailableException(
        "accounts-service rejected the credit with " + exception.getStatusCode(),
        exception
      );
    } catch (RestClientException exception) {
      throw new AccountsUnavailableException("Unable to reach accounts-service", exception);
    }
  }
}
