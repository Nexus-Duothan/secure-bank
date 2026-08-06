package com.securebank.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.securebank.user.dto.ServiceHealthResponse;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The panel this feeds used to claim every service was online without checking. These tests are
 * about the one thing that matters: what it reports must be what actually came back.
 */
@DisplayName("System health probe")
class ServiceHealthProbeTest {

  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  /** Starts a throwaway HTTP server standing in for one platform service. */
  private String startServer(int status, String body) throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/actuator/health", exchange -> {
      byte[] payload = body.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(status, payload.length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(payload);
      }
    });
    server.start();
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  /** Points every probed service at one address, so a single fake stands in for all of them. */
  private ServiceHealthProbe probeAgainst(String url) {
    return new ServiceHealthProbe(url, url, url, url, url, url, url, url, url);
  }

  private Map<String, ServiceHealthResponse> byKey(List<ServiceHealthResponse> results) {
    return results
      .stream()
      .collect(
        java.util.stream.Collectors.toMap(
          ServiceHealthResponse::key,
          Function.identity(),
          (first, second) -> first
        )
      );
  }

  @Test
  void reportsUpOnlyWhenTheServiceAnswersHealthy() throws IOException {
    String url = startServer(200, "{\"status\":\"UP\"}");

    Map<String, ServiceHealthResponse> results = byKey(probeAgainst(url).checkAll());

    assertThat(results.get("auth-service").status()).isEqualTo("UP");
    assertThat(results.get("auth-service").detail()).isNull();
    assertThat(results.get("auth-service").responseTimeMs()).isNotNull();
    assertThat(results.get("auth-service").checkedAt()).isNotNull();
  }

  @Test
  void reportsDownWhenNothingIsListeningAtAll() {
    // Port 1 is not bound; this is the case the old hardcoded panel got wrong.
    Map<String, ServiceHealthResponse> results = byKey(
      probeAgainst("http://127.0.0.1:1").checkAll()
    );

    assertThat(results.get("payments-service").status()).isEqualTo("DOWN");
    assertThat(results.get("payments-service").detail()).isNotBlank();
  }

  @Test
  void reportsDownWhenTheServiceAnswersAnErrorStatus() throws IOException {
    String url = startServer(503, "{\"status\":\"DOWN\"}");

    Map<String, ServiceHealthResponse> results = byKey(probeAgainst(url).checkAll());

    assertThat(results.get("lending-service").status()).isEqualTo("DOWN");
    assertThat(results.get("lending-service").detail()).contains("503");
  }

  @Test
  void reportsDownWhenAHealthyStatusCodeCarriesAnUnhealthyBody() throws IOException {
    // A service can be configured to answer 200 while reporting itself DOWN; the body decides.
    String url = startServer(
      200,
      "{\"status\":\"DOWN\",\"components\":{\"db\":{\"status\":\"DOWN\"}}}"
    );

    Map<String, ServiceHealthResponse> results = byKey(probeAgainst(url).checkAll());

    assertThat(results.get("accounts-service").status()).isEqualTo("DOWN");
  }

  @Test
  void reportsUnknownWhenNoAddressIsConfigured() throws IOException {
    String url = startServer(200, "{\"status\":\"UP\"}");
    ServiceHealthProbe probe = new ServiceHealthProbe(url, "", url, url, url, url, url, url, url);

    Map<String, ServiceHealthResponse> results = byKey(probe.checkAll());

    // Unconfigured is not the same as healthy, and not the same as down.
    assertThat(results.get("auth-service").status()).isEqualTo("UNKNOWN");
    assertThat(results.get("auth-service").detail()).contains("No address");
    assertThat(results.get("totp-service").status()).isEqualTo("UP");
  }

  @Test
  void coversEveryPlatformServiceAndReportsItselfWithoutACall() throws IOException {
    String url = startServer(200, "{\"status\":\"UP\"}");

    List<ServiceHealthResponse> results = probeAgainst(url).checkAll();

    assertThat(results).hasSize(10);
    assertThat(results.stream().map(ServiceHealthResponse::key)).containsExactlyInAnyOrder(
      "api-gateway",
      "auth-service",
      "totp-service",
      "user-service",
      "accounts-service",
      "transfer-service",
      "payments-service",
      "lending-service",
      "notification-service",
      "audit-recovery-service"
    );
    // user-service is the one answering, so it is up by definition and is not probed over HTTP.
    assertThat(byKey(results).get("user-service").status()).isEqualTo("UP");
  }
}
