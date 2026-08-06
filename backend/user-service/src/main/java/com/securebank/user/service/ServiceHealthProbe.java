package com.securebank.user.service;

import com.securebank.user.dto.ServiceHealthResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Asks every platform service how it is, one HTTP call each, and reports exactly what came back.
 *
 * <p>The admin System health panel used to be a hardcoded list that always read "Online", which
 * made it worse than having no panel: it stated a fact it had not checked. Everything here is
 * measured at the moment of the request.
 *
 * <p>Each service is probed on {@code /actuator/health}. A service is UP only when it answers 2xx
 * with a healthy body; anything else - unhealthy body, error status, refused connection, timeout -
 * is DOWN with the reason attached. A service with no configured address is UNKNOWN rather than
 * being quietly reported as either.
 */
@Service
@Slf4j
public class ServiceHealthProbe {

  /** Short on purpose: this is a dashboard, and a service that takes longer is not healthy. */
  private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(3);
  private static final String HEALTH_PATH = "/actuator/health";

  private final List<ProbeTarget> targets;
  private final HttpClient httpClient;

  public ServiceHealthProbe(
    @Value("${api-gateway.url:http://localhost:8080}") String apiGatewayUrl,
    @Value("${auth-service.url:http://localhost:8081}") String authServiceUrl,
    @Value("${totp-service.url:http://localhost:8082}") String totpServiceUrl,
    @Value("${accounts-service.url:http://localhost:8084}") String accountsServiceUrl,
    @Value("${transfer-service.url:http://localhost:8085}") String transferServiceUrl,
    @Value("${payments-service.url:http://localhost:8086}") String paymentsServiceUrl,
    @Value("${lending-service.url:http://localhost:8087}") String lendingServiceUrl,
    @Value("${notification-service.url:http://localhost:8088}") String notificationServiceUrl,
    @Value("${audit-service.url:http://localhost:8089}") String auditServiceUrl
  ) {
    this.httpClient = HttpClient.newBuilder()
      .connectTimeout(PROBE_TIMEOUT)
      .followRedirects(HttpClient.Redirect.NEVER)
      .build();
    this.targets = List.of(
      new ProbeTarget("api-gateway", "API Gateway", apiGatewayUrl),
      new ProbeTarget("auth-service", "Auth Service", authServiceUrl),
      new ProbeTarget("totp-service", "TOTP Service", totpServiceUrl),
      // This service is the one answering, so it is reachable by definition.
      new ProbeTarget("user-service", "User Service", null),
      new ProbeTarget("accounts-service", "Accounts Service", accountsServiceUrl),
      new ProbeTarget("transfer-service", "Transfer Service", transferServiceUrl),
      new ProbeTarget("payments-service", "Payments Service", paymentsServiceUrl),
      new ProbeTarget("lending-service", "Lending Service", lendingServiceUrl),
      new ProbeTarget("notification-service", "Notification Service", notificationServiceUrl),
      new ProbeTarget("audit-recovery-service", "Audit & Recovery Service", auditServiceUrl)
    );
  }

  /**
   * Probes every service at once, so the panel takes about as long as the slowest single service
   * rather than the sum of all of them.
   */
  public List<ServiceHealthResponse> checkAll() {
    try (ExecutorService pool = Executors.newFixedThreadPool(targets.size())) {
      List<CompletableFuture<ServiceHealthResponse>> checks = targets
        .stream()
        .map(target -> CompletableFuture.supplyAsync(() -> check(target), pool))
        .toList();
      return checks.stream().map(CompletableFuture::join).toList();
    }
  }

  private ServiceHealthResponse check(ProbeTarget target) {
    Instant startedAt = Instant.now();

    // user-service answers this request, so reaching its own port over HTTP would only prove the
    // network works. If this code is running, it is up.
    if (target.baseUrl() == null) {
      return new ServiceHealthResponse(target.key(), target.name(), "UP", null, 0L, startedAt);
    }
    if (target.baseUrl().isBlank()) {
      return new ServiceHealthResponse(
        target.key(),
        target.name(),
        "UNKNOWN",
        "No address is configured for this service",
        null,
        startedAt
      );
    }

    try {
      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(trimTrailingSlash(target.baseUrl()) + HEALTH_PATH))
        .timeout(PROBE_TIMEOUT)
        .GET()
        .build();

      HttpResponse<String> response = httpClient.send(
        request,
        HttpResponse.BodyHandlers.ofString()
      );
      long elapsedMs = Duration.between(startedAt, Instant.now()).toMillis();

      if (
        response.statusCode() >= 200 && response.statusCode() < 300 && reportsUp(response.body())
      ) {
        return new ServiceHealthResponse(
          target.key(),
          target.name(),
          "UP",
          null,
          elapsedMs,
          startedAt
        );
      }
      return new ServiceHealthResponse(
        target.key(),
        target.name(),
        "DOWN",
        "Answered HTTP " + response.statusCode(),
        elapsedMs,
        startedAt
      );
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return down(target, startedAt, "The health check was interrupted");
    } catch (Exception exception) {
      // Refused, timed out, unresolvable host: from an operator's point of view the service is
      // not serving, and the reason is what they need to see.
      log.debug("Health probe for {} failed: {}", target.key(), exception.toString());
      return down(target, startedAt, describe(exception));
    }
  }

  private ServiceHealthResponse down(ProbeTarget target, Instant startedAt, String detail) {
    return new ServiceHealthResponse(
      target.key(),
      target.name(),
      "DOWN",
      detail,
      Duration.between(startedAt, Instant.now()).toMillis(),
      startedAt
    );
  }

  /**
   * Actuator answers {@code {"status":"UP"}}; a service reporting DOWN with a 200 (possible when
   * the status mapping is customised) must not be counted as healthy, so the body is read too.
   */
  private boolean reportsUp(String body) {
    if (body == null || body.isBlank()) {
      // Some services answer 200 with an empty body; the status code is then all there is.
      return true;
    }
    String normalised = body.replaceAll("\\s", "").toUpperCase();
    if (
      normalised.contains("\"STATUS\":\"DOWN\"") ||
      normalised.contains("\"STATUS\":\"OUT_OF_SERVICE\"")
    ) {
      return false;
    }
    return true;
  }

  private String describe(Exception exception) {
    String message = exception.getMessage();
    String type = exception.getClass().getSimpleName();
    return message == null || message.isBlank() ? type : type + ": " + message;
  }

  private String trimTrailingSlash(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  /** @param baseUrl null means "this is the service running this code", so no HTTP call is made. */
  private record ProbeTarget(String key, String name, String baseUrl) {}
}
