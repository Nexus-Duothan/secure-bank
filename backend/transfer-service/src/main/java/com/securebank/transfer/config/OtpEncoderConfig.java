package com.securebank.transfer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * One-time codes are stored as adaptive hashes rather than plain text (NFR-S4), and verified with
 * BCrypt's constant-time comparison so confirmation timing leaks nothing about the code.
 */
@Configuration
public class OtpEncoderConfig {

  @Bean
  public PasswordEncoder otpEncoder() {
    return new BCryptPasswordEncoder();
  }
}
