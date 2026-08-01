package com.securebank.transfer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.securebank.transfer.config.TransferServiceProperties;
import com.securebank.transfer.dto.CreateScheduledTransferRequest;
import com.securebank.transfer.dto.ScheduledTransferResponse;
import com.securebank.transfer.dto.UpdateScheduleStatusRequest;
import com.securebank.transfer.entity.ScheduledTransfer;
import com.securebank.transfer.enums.ScheduleFrequency;
import com.securebank.transfer.enums.ScheduleStatus;
import com.securebank.transfer.repository.ScheduledTransferRepository;
import com.securebank.transfer.security.CallerIdentity;
import jakarta.persistence.EntityNotFoundException;
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
class ScheduledTransferServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final String FROM_ACCOUNT = "acc-demo-primary";
  private static final String TO_ACCOUNT = "acc-other";
  private static final CallerIdentity CALLER = new CallerIdentity(USER_ID);

  @Mock
  private ScheduledTransferRepository scheduledTransferRepository;

  private ScheduledTransferService scheduledTransferService;

  @BeforeEach
  void setUp() {
    TransferServiceProperties properties = new TransferServiceProperties(
      null,
      new TransferServiceProperties.Limits(new BigDecimal("1000"), new BigDecimal("1500"), null),
      null,
      null,
      null
    );
    scheduledTransferService = new ScheduledTransferService(scheduledTransferRepository, properties);
  }

  private CreateScheduledTransferRequest request(BigDecimal amount, Instant startAt, Instant endDate) {
    return new CreateScheduledTransferRequest(
      FROM_ACCOUNT,
      TO_ACCOUNT,
      amount,
      "rent",
      ScheduleFrequency.MONTHLY,
      startAt,
      endDate
    );
  }

  @Test
  void create_rejectsSameAccount() {
    assertThatThrownBy(() ->
      scheduledTransferService.create(
        CALLER,
        new CreateScheduledTransferRequest(
          FROM_ACCOUNT,
          FROM_ACCOUNT,
          BigDecimal.TEN,
          null,
          ScheduleFrequency.ONE_TIME,
          Instant.now().plusSeconds(3600),
          null
        )
      )
    ).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void create_rejectsAmountAboveThePerTransactionLimit() {
    assertThatThrownBy(() ->
      scheduledTransferService.create(
        CALLER,
        request(new BigDecimal("1500"), Instant.now().plusSeconds(3600), null)
      )
    ).isInstanceOf(LimitExceededException.class);
  }

  @Test
  void create_rejectsStartAtInThePast() {
    assertThatThrownBy(() ->
      scheduledTransferService.create(
        CALLER,
        request(new BigDecimal("100"), Instant.now().minusSeconds(3600), null)
      )
    ).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void create_rejectsEndDateBeforeStartAt() {
    Instant startAt = Instant.now().plusSeconds(3600);
    assertThatThrownBy(() ->
      scheduledTransferService.create(CALLER, request(new BigDecimal("100"), startAt, startAt.minusSeconds(60)))
    ).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void create_savesActiveScheduleWithNextRunAtEqualToStartAt() {
    when(scheduledTransferRepository.save(any(ScheduledTransfer.class))).thenAnswer(
      invocation -> invocation.getArgument(0)
    );
    Instant startAt = Instant.now().plusSeconds(3600);

    ScheduledTransferResponse response = scheduledTransferService.create(
      CALLER,
      request(new BigDecimal("100"), startAt, null)
    );

    assertThat(response.status()).isEqualTo(ScheduleStatus.ACTIVE);
    assertThat(response.nextRunAt()).isEqualTo(startAt);
  }

  @Test
  void updateStatus_throwsNotFound_whenNotOwnedByCaller() {
    when(scheduledTransferRepository.findByIdAndOwnerUserId(any(), any())).thenReturn(Optional.empty());

    assertThatThrownBy(() ->
      scheduledTransferService.updateStatus(
        CALLER,
        UUID.randomUUID(),
        new UpdateScheduleStatusRequest(ScheduleStatus.PAUSED)
      )
    ).isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void updateStatus_rejectsReactivatingACancelledSchedule() {
    ScheduledTransfer schedule = ScheduledTransfer.builder()
      .id(UUID.randomUUID())
      .ownerUserId(USER_ID)
      .status(ScheduleStatus.CANCELLED)
      .build();
    when(scheduledTransferRepository.findByIdAndOwnerUserId(schedule.getId(), USER_ID)).thenReturn(
      Optional.of(schedule)
    );

    assertThatThrownBy(() ->
      scheduledTransferService.updateStatus(
        CALLER,
        schedule.getId(),
        new UpdateScheduleStatusRequest(ScheduleStatus.ACTIVE)
      )
    ).isInstanceOf(ConflictException.class);
  }

  @Test
  void updateStatus_rejectsSystemOnlyTargetStatus() {
    assertThatThrownBy(() ->
      scheduledTransferService.updateStatus(
        CALLER,
        UUID.randomUUID(),
        new UpdateScheduleStatusRequest(ScheduleStatus.COMPLETED)
      )
    ).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void updateStatus_pausesAnActiveSchedule() {
    ScheduledTransfer schedule = ScheduledTransfer.builder()
      .id(UUID.randomUUID())
      .ownerUserId(USER_ID)
      .status(ScheduleStatus.ACTIVE)
      .build();
    when(scheduledTransferRepository.findByIdAndOwnerUserId(schedule.getId(), USER_ID)).thenReturn(
      Optional.of(schedule)
    );
    when(scheduledTransferRepository.save(any(ScheduledTransfer.class))).thenAnswer(
      invocation -> invocation.getArgument(0)
    );

    ScheduledTransferResponse response = scheduledTransferService.updateStatus(
      CALLER,
      schedule.getId(),
      new UpdateScheduleStatusRequest(ScheduleStatus.PAUSED)
    );

    assertThat(response.status()).isEqualTo(ScheduleStatus.PAUSED);
  }
}
