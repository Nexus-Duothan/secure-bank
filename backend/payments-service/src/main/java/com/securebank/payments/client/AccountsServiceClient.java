package com.securebank.payments.client;

import com.securebank.payments.client.dto.AccountDebitRequest;
import com.securebank.payments.client.dto.AccountDebitResponse;
import com.securebank.payments.exception.AccountsServiceUnavailableException;
import com.securebank.payments.exception.InsufficientFundsException;
import com.securebank.payments.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * accounts-service does not exist yet (bare scaffold as of this writing), so this client
 * has nothing live to call. It's built against the contract documented in README.md
 * ("Accounts Service Integration") so it's ready once that service ships: POST
 * /api/v1/accounts/{accountId}/debit -> 200 (new balance), 404 (unknown account), 409
 * (insufficient funds). Not final so a test-profile subclass can override debit().
 */
@Component
public class AccountsServiceClient {

  private final WebClient webClient;

  public AccountsServiceClient(
    WebClient.Builder webClientBuilder,
    @Value("${accounts-service.base-url}") String baseUrl
  ) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  public AccountDebitResponse debit(String accountId, AccountDebitRequest request) {
    try {
      return webClient
        .post()
        .uri("/api/v1/accounts/{accountId}/debit", accountId)
        .bodyValue(request)
        .retrieve()
        .onStatus(
          status -> status.value() == 404,
          response -> Mono.error(new ResourceNotFoundException("Account not found: " + accountId))
        )
        .onStatus(
          status -> status.value() == 409,
          response ->
            Mono.error(new InsufficientFundsException("Insufficient funds in account " + accountId))
        )
        .bodyToMono(AccountDebitResponse.class)
        .block();
    } catch (WebClientRequestException ex) {
      throw new AccountsServiceUnavailableException("Accounts service is unavailable", ex);
    } catch (WebClientResponseException ex) {
      throw new AccountsServiceUnavailableException(
        "Accounts service returned an unexpected error: " + ex.getStatusCode(),
        ex
      );
    }
  }
}
