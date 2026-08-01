package com.securebank.user.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Externalised tuning for the User &amp; RBAC service (see {@code application.yml}). */
@ConfigurationProperties(prefix = "securebank.user")
public record UserServiceProperties(boolean seedDemoData, Cors cors, Otp otp, Security security) {
  public UserServiceProperties {
    cors = cors == null ? new Cors(null) : cors;
    otp = otp == null ? new Otp(null, 0, false) : otp;
    security = security == null ? new Security(false) : security;
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
   * @param ttl how long a challenge stays valid
   * @param maxAttempts wrong codes tolerated before the challenge is burned (blocks brute force
   *     of the six digit space, FR-33)
   * @param exposeCode local-prototype switch that returns the generated code to the caller
   */
  public record Otp(Duration ttl, int maxAttempts, boolean exposeCode) {
    public Otp {
      ttl = ttl == null ? Duration.ofMinutes(5) : ttl;
      maxAttempts = maxAttempts <= 0 ? 5 : maxAttempts;
    }
  }

  public record Security(boolean allowUnauthenticatedDemoCaller) {}
}
