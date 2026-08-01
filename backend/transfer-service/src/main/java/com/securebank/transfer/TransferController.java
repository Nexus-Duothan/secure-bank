package com.securebank.transfer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

  @PostMapping("/")
  public CreateTransferResponse createTransfer(@RequestBody CreateTransferRequest request) {
    return new CreateTransferResponse(
      "trf-" + UUID.randomUUID(),
      "QUEUED",
      BigDecimal.ZERO,
      "LKR",
      OffsetDateTime.now().toString()
    );
  }

  public record CreateTransferRequest(
    String fromAccountId,
    String toAccount,
    BigDecimal amount,
    String note
  ) {}

  public record CreateTransferResponse(
    String id,
    String status,
    BigDecimal fee,
    String currency,
    String createdAt
  ) {}
}
