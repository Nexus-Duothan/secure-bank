package com.securebank.payments.client;

import com.securebank.payments.client.dto.AccountDebitRequest;
import com.securebank.payments.client.dto.AccountDebitResponse;
import com.securebank.payments.client.dto.AccountRefundRequest;
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
 * Debits the payer's account in accounts-service, which owns the ledger: POST
 * /internal/v1/accounts/by-user/{userId}/debit -> 200 (new balance), 404 (the customer has no
 * account), 409 (insufficient funds or frozen account). A vendor payment names the payer, not an
 * account, so accounts-service resolves it to that customer's primary account. The
 * {@code /internal} routes are not published by the API gateway, so only services inside the
 * cluster can move money this way. Not final so a test-profile subclass can override debit().
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

  public AccountDebitResponse debit(String payerUserId, AccountDebitRequest request) {
    try {
      return webClient
        .post()
        .uri("/internal/v1/accounts/by-user/{userId}/debit", payerUserId)
        .bodyValue(request)
        .retrieve()
        .onStatus(
          status -> status.value() == 404,
          response ->
            Mono.error(
              new ResourceNotFoundException("No account found for customer " + payerUserId)
            )
        )
        .onStatus(
          status -> status.value() == 409,
          response ->
            Mono.error(
              new InsufficientFundsException("Insufficient funds for customer " + payerUserId)
            )
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

  public AccountDebitResponse debitAccount(
    String payerUserId,
    String accountId,
    AccountDebitRequest request
  ) {
    try {
      return webClient
        .post()
        .uri(
          "/internal/v1/accounts/by-user/{userId}/accounts/{accountId}/debit",
          payerUserId,
          accountId
        )
        .bodyValue(request)
        .retrieve()
        .onStatus(
          status -> status.value() == 404,
          response ->
            Mono.error(new ResourceNotFoundException("Account not found for this customer"))
        )
        .onStatus(
          status -> status.value() == 409,
          response ->
            Mono.error(new InsufficientFundsException("Insufficient funds or account is frozen"))
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

  public void refund(AccountRefundRequest request) {
    try {
      webClient
        .post()
        .uri("/internal/v1/accounts/refund")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(Void.class)
        .block();
    } catch (WebClientRequestException ex) {
      throw new AccountsServiceUnavailableException("Accounts service is unavailable", ex);
    } catch (WebClientResponseException ex) {
      if (ex.getStatusCode().value() == 409) {
        throw new InsufficientFundsException(
          "The merchant settlement account cannot fund this refund"
        );
      }
      throw new AccountsServiceUnavailableException(
        "Accounts service returned an unexpected error: " + ex.getStatusCode(),
        ex
      );
    }
  }
}
