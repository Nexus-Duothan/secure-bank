package com.securebank.transfer.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Externalised tuning for the Transfer service (see {@code application.yml}). */
@ConfigurationProperties(prefix = "securebank.transfer")
public record TransferServiceProperties(
  Cors cors,
  Limits limits,
  AccountsClient accountsClient,
  Security security,
  Otp otp
) {
  public TransferServiceProperties {
    cors = cors == null ? new Cors(null) : cors;
    limits = limits == null ? new Limits(null, null, null) : limits;
    accountsClient = accountsClient == null ? new AccountsClient(null) : accountsClient;
    security = security == null ? new Security(false, null) : security;
    otp = otp == null ? new Otp(null, 0, false) : otp;
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
   * @param payeeCoolingOffThreshold amount at or above which a transfer to a payee still inside
   *     its 12-hour cooling-off window is rejected (FR-16)
   */
  public record Limits(BigDecimal perTransaction, BigDecimal daily, BigDecimal payeeCoolingOffThreshold) {
    public Limits {
      perTransaction = perTransaction == null ? new BigDecimal("500000") : perTransaction;
      daily = daily == null ? new BigDecimal("1000000") : daily;
      payeeCoolingOffThreshold =
        payeeCoolingOffThreshold == null ? new BigDecimal("50000") : payeeCoolingOffThreshold;
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

  /**
   * @param ttl how long an add-payee OTP challenge stays valid
   * @param maxAttempts wrong codes tolerated before the challenge is burned
   * @param exposeCode local-prototype switch that returns the generated code to the caller
   */
  public record Otp(Duration ttl, int maxAttempts, boolean exposeCode) {
    public Otp {
      ttl = ttl == null ? Duration.ofMinutes(5) : ttl;
      maxAttempts = maxAttempts <= 0 ? 5 : maxAttempts;
    }
  }
}
