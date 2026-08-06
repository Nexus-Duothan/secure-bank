package com.securebank.payments.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Generates the cryptographically reference-numbered digital receipt for a payment. */
@Component
public class ReceiptReferenceGenerator {

  private final String secret;

  public ReceiptReferenceGenerator(@Value("${payments.receipt-secret}") String secret) {
    this.secret = secret;
  }

  public String generate(UUID paymentId, Instant issuedAt) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      String raw = paymentId + ":" + issuedAt.toEpochMilli() + ":" + secret;
      byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));

      StringBuilder hex = new StringBuilder();
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return "RCPT-" + hex.substring(0, 16).toUpperCase();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 algorithm not available", ex);
    }
  }
}
