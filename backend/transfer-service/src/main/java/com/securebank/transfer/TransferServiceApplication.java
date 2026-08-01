package com.securebank.transfer;

import com.securebank.transfer.config.TransferServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TransferServiceProperties.class)
public class TransferServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(TransferServiceApplication.class, args);
  }
}
