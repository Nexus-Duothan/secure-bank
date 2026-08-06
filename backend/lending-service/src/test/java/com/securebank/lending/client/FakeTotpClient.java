package com.securebank.lending.client;

import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Primary
@Profile("test")
public class FakeTotpClient implements TotpClient {

  @Override
  public boolean verify(UUID userId, String code) {
    return "123456".equals(code);
  }
}
