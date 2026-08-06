package com.securebank.payments.client;

import java.util.UUID;

public interface TotpClient {
  boolean verify(UUID userId, String code);
}
