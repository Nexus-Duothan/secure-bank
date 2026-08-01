package com.securebank.payments.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.payments.dto.QrPaymentDetails;
import com.securebank.payments.exception.InvalidQrPayloadException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Decodes a SecureBank merchant QR payload: a base64-encoded JSON object of the form
 * {"merchantCode":"MCH-1029","suggestedAmount":1500.00,"currency":"LKR"}. This format is
 * invented for this service (no prior convention exists) — the frontend must generate
 * matching QR codes.
 */
@Component
public class QrCodeDecoder {

  private final ObjectMapper objectMapper;

  public QrCodeDecoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public QrPaymentDetails decode(String base64Payload) {
    try {
      byte[] decoded = Base64.getDecoder().decode(base64Payload);
      String json = new String(decoded, StandardCharsets.UTF_8);
      return objectMapper.readValue(json, QrPaymentDetails.class);
    } catch (Exception ex) {
      throw new InvalidQrPayloadException("Unable to decode QR payment payload");
    }
  }
}
