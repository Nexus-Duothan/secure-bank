package com.securebank.lending;

import com.securebank.lending.config.LendingServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(LendingServiceProperties.class)
public class LendingServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(LendingServiceApplication.class, args);
  }
}
