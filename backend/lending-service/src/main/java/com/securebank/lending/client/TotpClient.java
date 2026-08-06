package com.securebank.lending.client;

import java.util.UUID;

public interface TotpClient {
  boolean verify(UUID userId, String code);
}
