package com.securebank.transfer.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "transfer_daily_usage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(TransferDailyUsage.Key.class)
public class TransferDailyUsage {

  @Id
  @Column(name = "account_id", nullable = false, length = 64)
  private String accountId;

  @Id
  @Column(name = "usage_date", nullable = false)
  private LocalDate usageDate;

  @Builder.Default
  @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Key implements Serializable {

    private String accountId;
    private LocalDate usageDate;
  }
}
