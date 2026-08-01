package com.securebank.payments;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentsController {

  @PostMapping("/bills")
  public PayBillResponse payBill(@RequestBody PayBillRequest request) {
    return new PayBillResponse(
      "pay-" + UUID.randomUUID(),
      "SCHEDULED",
      "LKR",
      OffsetDateTime.now().toString()
    );
  }

  public record PayBillRequest(
    String billerCategory,
    String billerName,
    String referenceNumber,
    BigDecimal amount,
    String fromAccountId
  ) {}

  public record PayBillResponse(String id, String status, String currency, String createdAt) {}
}
