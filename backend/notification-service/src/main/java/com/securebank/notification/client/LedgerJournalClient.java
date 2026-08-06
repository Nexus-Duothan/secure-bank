package com.securebank.notification.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Reads the bank-wide transaction journal from accounts-service, which owns the ledger. Going to
 * the source means the audit view can never drift from the movements that actually posted.
 */
@Component
@Slf4j
public class LedgerJournalClient {

  private final RestClient restClient;

  public LedgerJournalClient(
    @Value("${accounts-service.base-url:http://localhost:8084}") String baseUrl
  ) {
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  public List<JournalEntry> getJournal(int limit) {
    try {
      List<JournalEntry> entries = restClient
        .get()
        .uri("/internal/v1/accounts/journal?limit={limit}", limit)
        .retrieve()
        .body(new ParameterizedTypeReference<List<JournalEntry>>() {});
      return entries == null ? List.of() : entries;
    } catch (Exception exception) {
      log.warn(
        "Could not read the transaction journal from accounts-service: {}",
        exception.getMessage()
      );
      return List.of();
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record JournalEntry(
    String id,
    String merchant,
    String category,
    String transactionType,
    String location,
    BigDecimal amount,
    String currency,
    Instant timestamp,
    String journalId,
    boolean flagged
  ) {}
}
