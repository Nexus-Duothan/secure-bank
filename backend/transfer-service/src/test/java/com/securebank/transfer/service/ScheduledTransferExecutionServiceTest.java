package com.securebank.transfer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.securebank.transfer.dto.TransferQuoteRequest;
import com.securebank.transfer.dto.TransferResponse;
import com.securebank.transfer.entity.ScheduledTransfer;
import com.securebank.transfer.enums.ScheduleFrequency;
import com.securebank.transfer.enums.ScheduleStatus;
import com.securebank.transfer.enums.TransferStatus;
import com.securebank.transfer.repository.ScheduledTransferRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduledTransferExecutionServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();

  @Mock
  private ScheduledTransferRepository scheduledTransferRepository;

  @Mock
  private TransferService transferService;

  private ScheduledTransferExecutionService executionService;

  @BeforeEach
  void setUp() {
    executionService = new ScheduledTransferExecutionService(
      scheduledTransferRepository,
      transferService
    );
  }

  private ScheduledTransfer.ScheduledTransferBuilder dueSchedule(ScheduleFrequency frequency) {
    return ScheduledTransfer.builder()
      .id(UUID.randomUUID())
      .ownerUserId(USER_ID)
      .fromAccountId("acc-demo-primary")
      .toAccount("acc-other")
      .amount(new BigDecimal("100"))
      .frequency(frequency)
      .nextRunAt(Instant.now().minusSeconds(60))
      .status(ScheduleStatus.ACTIVE);
  }

  private TransferResponse completedTransfer() {
    return new TransferResponse(
      UUID.randomUUID(),
      TransferStatus.COMPLETED,
      "acc-demo-primary",
      "acc-other",
      new BigDecimal("100"),
      BigDecimal.ZERO,
      new BigDecimal("100"),
      "LKR",
      null,
      null,
      Instant.now(),
      Instant.now()
    );
  }

  private TransferResponse pendingTransfer(UUID id) {
    return new TransferResponse(
      id,
      TransferStatus.PENDING_CONFIRMATION,
      "acc-demo-primary",
      "acc-other",
      new BigDecimal("100"),
      BigDecimal.ZERO,
      new BigDecimal("100"),
      "LKR",
      null,
      null,
      Instant.now(),
      null
    );
  }

  private TransferResponse failedTransfer() {
    return new TransferResponse(
      UUID.randomUUID(),
      TransferStatus.FAILED,
      "acc-demo-primary",
      "acc-other",
      new BigDecimal("100"),
      BigDecimal.ZERO,
      new BigDecimal("100"),
      "LKR",
      null,
      "Insufficient balance at confirmation time",
      Instant.now(),
      null
    );
  }

  @Test
  void executeDue_doesNothing_whenScheduleNoLongerActive() {
    ScheduledTransfer schedule = dueSchedule(ScheduleFrequency.ONE_TIME)
      .status(ScheduleStatus.PAUSED)
      .build();
    when(scheduledTransferRepository.findForUpdateById(schedule.getId())).thenReturn(
      Optional.of(schedule)
    );

    executionService.executeDue(schedule.getId());

    verify(transferService, never()).quote(any(), any(), anyString());
    verify(scheduledTransferRepository, never()).save(any());
  }

  @Test
  void executeDue_doesNothing_whenNotActuallyDueYet() {
    ScheduledTransfer schedule = dueSchedule(ScheduleFrequency.ONE_TIME)
      .nextRunAt(Instant.now().plusSeconds(3600))
      .build();
    when(scheduledTransferRepository.findForUpdateById(schedule.getId())).thenReturn(
      Optional.of(schedule)
    );

    executionService.executeDue(schedule.getId());

    verify(transferService, never()).quote(any(), any(), anyString());
  }

  @Test
  void executeDue_marksOneTimeScheduleCompleted_onSuccess() {
    ScheduledTransfer schedule = dueSchedule(ScheduleFrequency.ONE_TIME).build();
    when(scheduledTransferRepository.findForUpdateById(schedule.getId())).thenReturn(
      Optional.of(schedule)
    );
    UUID transferId = UUID.randomUUID();
    when(transferService.quote(any(), any(), anyString())).thenReturn(pendingTransfer(transferId));
    when(transferService.confirm(any(), eq(transferId))).thenReturn(completedTransfer());
    when(scheduledTransferRepository.save(any(ScheduledTransfer.class))).thenAnswer(invocation ->
      invocation.getArgument(0)
    );

    executionService.executeDue(schedule.getId());

    assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.COMPLETED);
    assertThat(schedule.getLastExecutionStatus()).isEqualTo("COMPLETED");
  }

  @Test
  void executeDue_marksOneTimeScheduleFailed_whenTransferFails() {
    ScheduledTransfer schedule = dueSchedule(ScheduleFrequency.ONE_TIME).build();
    when(scheduledTransferRepository.findForUpdateById(schedule.getId())).thenReturn(
      Optional.of(schedule)
    );
    UUID transferId = UUID.randomUUID();
    when(transferService.quote(any(), any(), anyString())).thenReturn(pendingTransfer(transferId));
    when(transferService.confirm(any(), eq(transferId))).thenReturn(failedTransfer());
    when(scheduledTransferRepository.save(any(ScheduledTransfer.class))).thenAnswer(invocation ->
      invocation.getArgument(0)
    );

    executionService.executeDue(schedule.getId());

    assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.FAILED);
    assertThat(schedule.getLastExecutionStatus()).contains("Insufficient balance");
  }

  @Test
  void executeDue_advancesWeeklyScheduleAndStaysActive_onSuccess() {
    ScheduledTransfer schedule = dueSchedule(ScheduleFrequency.WEEKLY).build();
    Instant originalNextRun = schedule.getNextRunAt();
    when(scheduledTransferRepository.findForUpdateById(schedule.getId())).thenReturn(
      Optional.of(schedule)
    );
    UUID transferId = UUID.randomUUID();
    when(transferService.quote(any(), any(), anyString())).thenReturn(pendingTransfer(transferId));
    when(transferService.confirm(any(), eq(transferId))).thenReturn(completedTransfer());
    when(scheduledTransferRepository.save(any(ScheduledTransfer.class))).thenAnswer(invocation ->
      invocation.getArgument(0)
    );

    executionService.executeDue(schedule.getId());

    assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.ACTIVE);
    assertThat(schedule.getNextRunAt()).isEqualTo(originalNextRun.plusSeconds(7 * 24 * 3600));
  }

  @Test
  void executeDue_advancesWeeklyScheduleAndStaysActive_evenWhenExecutionFails() {
    ScheduledTransfer schedule = dueSchedule(ScheduleFrequency.WEEKLY).build();
    Instant originalNextRun = schedule.getNextRunAt();
    when(scheduledTransferRepository.findForUpdateById(schedule.getId())).thenReturn(
      Optional.of(schedule)
    );
    UUID transferId = UUID.randomUUID();
    when(transferService.quote(any(), any(), anyString())).thenReturn(pendingTransfer(transferId));
    when(transferService.confirm(any(), eq(transferId))).thenReturn(failedTransfer());
    when(scheduledTransferRepository.save(any(ScheduledTransfer.class))).thenAnswer(invocation ->
      invocation.getArgument(0)
    );

    executionService.executeDue(schedule.getId());

    assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.ACTIVE);
    assertThat(schedule.getNextRunAt()).isEqualTo(originalNextRun.plusSeconds(7 * 24 * 3600));
    assertThat(schedule.getLastExecutionStatus()).contains("FAILED");
  }

  @Test
  void executeDue_completesRecurringSchedule_whenNextOccurrenceWouldPassEndDate() {
    ScheduledTransfer schedule = dueSchedule(ScheduleFrequency.WEEKLY)
      .endDate(Instant.now().plusSeconds(3600))
      .build();
    when(scheduledTransferRepository.findForUpdateById(schedule.getId())).thenReturn(
      Optional.of(schedule)
    );
    UUID transferId = UUID.randomUUID();
    when(transferService.quote(any(), any(), anyString())).thenReturn(pendingTransfer(transferId));
    when(transferService.confirm(any(), eq(transferId))).thenReturn(completedTransfer());
    when(scheduledTransferRepository.save(any(ScheduledTransfer.class))).thenAnswer(invocation ->
      invocation.getArgument(0)
    );

    executionService.executeDue(schedule.getId());

    assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.COMPLETED);
  }

  @Test
  void executeDue_usesOccurrenceScopedIdempotencyKey() {
    ScheduledTransfer schedule = dueSchedule(ScheduleFrequency.ONE_TIME).build();
    when(scheduledTransferRepository.findForUpdateById(schedule.getId())).thenReturn(
      Optional.of(schedule)
    );
    UUID transferId = UUID.randomUUID();
    when(transferService.quote(any(), any(), anyString())).thenReturn(pendingTransfer(transferId));
    when(transferService.confirm(any(), eq(transferId))).thenReturn(completedTransfer());
    when(scheduledTransferRepository.save(any(ScheduledTransfer.class))).thenAnswer(invocation ->
      invocation.getArgument(0)
    );

    executionService.executeDue(schedule.getId());

    String expectedKey = "scheduled:" + schedule.getId() + ":" + schedule.getNextRunAt();
    verify(transferService).quote(any(), any(TransferQuoteRequest.class), eq(expectedKey));
  }
}
