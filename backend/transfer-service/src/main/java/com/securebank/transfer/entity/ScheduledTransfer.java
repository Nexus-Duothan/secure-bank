package com.securebank.transfer.entity;

import com.securebank.transfer.enums.ScheduleFrequency;
import com.securebank.transfer.enums.ScheduleStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "scheduled_transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledTransfer {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "owner_user_id", nullable = false)
  private UUID ownerUserId;

  @Column(name = "from_account_id", nullable = false, length = 64)
  private String fromAccountId;

  @Column(name = "to_account", nullable = false, length = 64)
  private String toAccount;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(length = 280)
  private String note;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ScheduleFrequency frequency;

  @Column(name = "next_run_at", nullable = false)
  private Instant nextRunAt;

  @Column(name = "end_date")
  private Instant endDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ScheduleStatus status;

  @Column(name = "last_executed_at")
  private Instant lastExecutedAt;

  @Column(name = "last_execution_status", length = 280)
  private String lastExecutionStatus;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = Instant.now();
  }
}
