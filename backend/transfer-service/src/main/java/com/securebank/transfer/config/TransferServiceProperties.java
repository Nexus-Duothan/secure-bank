package com.securebank.transfer.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Externalised tuning for the Transfer service (see {@code application.yml}). */
@ConfigurationProperties(prefix = "securebank.transfer")
public record TransferServiceProperties(
  Cors cors,
  Limits limits,
  AccountsClient accountsClient,
  Security security
) {
  public TransferServiceProperties {
    cors = cors == null ? new Cors(null) : cors;
    limits = limits == null ? new Limits(null, null) : limits;
    accountsClient = accountsClient == null ? new AccountsClient(null) : accountsClient;
    security = security == null ? new Security(false, null) : security;
  }

  public record Cors(List<String> allowedOrigins) {
    public Cors {
      allowedOrigins =
        allowedOrigins == null || allowedOrigins.isEmpty()
          ? List.of("http://localhost:3000", "http://localhost:5173")
          : List.copyOf(allowedOrigins);
    }
  }

  /**
   * @param perTransaction maximum amount allowed in a single transfer (FR-18)
   * @param daily maximum aggregate amount allowed from one account per calendar day (FR-18)
   */
  public record Limits(BigDecimal perTransaction, BigDecimal daily) {
    public Limits {
      perTransaction = perTransaction == null ? new BigDecimal("500000") : perTransaction;
      daily = daily == null ? new BigDecimal("1000000") : daily;
    }
  }

  public record AccountsClient(String baseUrl) {
    public AccountsClient {
      baseUrl = baseUrl == null || baseUrl.isBlank() ? "http://localhost:8084" : baseUrl;
    }
  }

  public record Security(boolean allowUnauthenticatedDemoCaller, UUID demoUserId) {
    public Security {
      demoUserId =
        demoUserId == null ? UUID.fromString("00000000-0000-0000-0000-000000000001") : demoUserId;
    }
  }
}
