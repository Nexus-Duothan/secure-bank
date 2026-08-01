package com.securebank.lending.client;

import com.securebank.lending.config.LendingServiceProperties;
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
        .uri("/api/v1/accounts/{id}", accountId)
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
}
