package com.securebank.transfer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.securebank.transfer.config.TransferServiceProperties;
import com.securebank.transfer.dto.AddPayeeRequest;
import com.securebank.transfer.dto.ConfirmPayeeRequest;
import com.securebank.transfer.dto.EditPayeeRequest;
import com.securebank.transfer.dto.PayeeChallengeResponse;
import com.securebank.transfer.dto.PayeeResponse;
import com.securebank.transfer.entity.Payee;
import com.securebank.transfer.entity.PendingPayeeAddition;
import com.securebank.transfer.repository.PayeeRepository;
import com.securebank.transfer.repository.PendingPayeeAdditionRepository;
import com.securebank.transfer.security.CallerIdentity;
import com.securebank.transfer.service.notification.PayeeAlertService;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PayeeServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final String KNOWN_CODE = "123456";
  private static final int MAX_ATTEMPTS = 3;
  private static final CallerIdentity CALLER = new CallerIdentity(USER_ID);

  @Mock
  private PayeeRepository payeeRepository;

  @Mock
  private PendingPayeeAdditionRepository pendingPayeeAdditionRepository;

  @Mock
  private PayeeAlertService payeeAlertService;

  private final PasswordEncoder otpEncoder = new BCryptPasswordEncoder();

  private PayeeService payeeService;

  @BeforeEach
  void setUp() {
    TransferServiceProperties properties = new TransferServiceProperties(
      null,
      null,
      null,
      null,
      new TransferServiceProperties.Otp(Duration.ofMinutes(5), MAX_ATTEMPTS, true)
    );
    payeeService = new PayeeService(
      payeeRepository,
      pendingPayeeAdditionRepository,
      otpEncoder,
      properties,
      payeeAlertService
    );
  }

  private PendingPayeeAddition.PendingPayeeAdditionBuilder pendingAddition() {
    return PendingPayeeAddition.builder()
      .id(UUID.randomUUID())
      .ownerUserId(USER_ID)
      .nickname("A. Silva")
      .accountReference("acc-other")
      .otpHash(otpEncoder.encode(KNOWN_CODE))
      .expiresAt(Instant.now().plusSeconds(300))
      .failedAttempts(0)
      .confirmed(false);
  }

  @Test
  void requestAddPayee_rejectsWhenAccountAlreadySaved() {
    when(payeeRepository.existsByOwnerUserIdAndAccountReferenceIgnoreCase(USER_ID, "acc-other"))
      .thenReturn(true);

    assertThatThrownBy(() ->
      payeeService.requestAddPayee(CALLER, new AddPayeeRequest("A. Silva", "acc-other"))
    ).isInstanceOf(ConflictException.class);
  }

  @Test
  void requestAddPayee_stagesChallengeAndExposesCodeWhenConfigured() {
    when(pendingPayeeAdditionRepository.save(any(PendingPayeeAddition.class))).thenAnswer(
      invocation -> invocation.getArgument(0)
    );

    PayeeChallengeResponse response = payeeService.requestAddPayee(
      CALLER,
      new AddPayeeRequest("A. Silva", "acc-other")
    );

    assertThat(response.demoCode()).isNotBlank();
    assertThat(response.demoCode()).hasSize(6);
  }

  @Test
  void confirmAddPayee_throwsNotFound_whenChangeDoesNotBelongToCaller() {
    when(pendingPayeeAdditionRepository.findByIdAndOwnerUserId(any(), any())).thenReturn(
      Optional.empty()
    );

    assertThatThrownBy(() ->
      payeeService.confirmAddPayee(CALLER, UUID.randomUUID(), new ConfirmPayeeRequest(KNOWN_CODE))
    ).isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void confirmAddPayee_rejectsWrongCode_andIncrementsFailedAttempts() {
    PendingPayeeAddition change = pendingAddition().build();
    when(pendingPayeeAdditionRepository.findByIdAndOwnerUserId(change.getId(), USER_ID))
      .thenReturn(Optional.of(change));

    assertThatThrownBy(() ->
      payeeService.confirmAddPayee(CALLER, change.getId(), new ConfirmPayeeRequest("000000"))
    ).isInstanceOf(OtpVerificationException.class);

    assertThat(change.getFailedAttempts()).isEqualTo(1);
    verify(pendingPayeeAdditionRepository).save(change);
    verify(payeeRepository, never()).save(any());
  }

  @Test
  void confirmAddPayee_rejectsExpiredChallenge() {
    PendingPayeeAddition change = pendingAddition().expiresAt(Instant.now().minusSeconds(1)).build();
    when(pendingPayeeAdditionRepository.findByIdAndOwnerUserId(change.getId(), USER_ID))
      .thenReturn(Optional.of(change));

    assertThatThrownBy(() ->
      payeeService.confirmAddPayee(CALLER, change.getId(), new ConfirmPayeeRequest(KNOWN_CODE))
    ).isInstanceOf(OtpVerificationException.class);
  }

  @Test
  void confirmAddPayee_rejectsAfterAttemptsExhausted() {
    PendingPayeeAddition change = pendingAddition().failedAttempts(MAX_ATTEMPTS).build();
    when(pendingPayeeAdditionRepository.findByIdAndOwnerUserId(change.getId(), USER_ID))
      .thenReturn(Optional.of(change));

    assertThatThrownBy(() ->
      payeeService.confirmAddPayee(CALLER, change.getId(), new ConfirmPayeeRequest(KNOWN_CODE))
    ).isInstanceOf(OtpVerificationException.class);
  }

  @Test
  void confirmAddPayee_createsPayeeWithTwelveHourCoolingOff_whenCodeIsCorrect() {
    PendingPayeeAddition change = pendingAddition().build();
    when(pendingPayeeAdditionRepository.findByIdAndOwnerUserId(change.getId(), USER_ID))
      .thenReturn(Optional.of(change));
    when(payeeRepository.save(any(Payee.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Instant before = Instant.now();
    PayeeResponse response = payeeService.confirmAddPayee(
      CALLER,
      change.getId(),
      new ConfirmPayeeRequest(KNOWN_CODE)
    );

    assertThat(response.nickname()).isEqualTo("A. Silva");
    assertThat(response.accountReference()).isEqualTo("acc-other");
    assertThat(response.coolingOff()).isTrue();
    assertThat(response.coolingOffUntil()).isAfter(before.plusSeconds(11 * 3600));
    assertThat(change.isConfirmed()).isTrue();
    verify(payeeAlertService).sendPayeeAddedAlert(any());
  }

  @Test
  void editPayee_updatesNickname_whenOwnedByCaller() {
    Payee payee = Payee.builder()
      .id(UUID.randomUUID())
      .ownerUserId(USER_ID)
      .nickname("Old name")
      .accountReference("acc-other")
      .coolingOffUntil(Instant.now())
      .build();
    when(payeeRepository.findByIdAndOwnerUserId(payee.getId(), USER_ID)).thenReturn(Optional.of(payee));
    when(payeeRepository.save(any(Payee.class))).thenAnswer(invocation -> invocation.getArgument(0));

    PayeeResponse response = payeeService.editPayee(CALLER, payee.getId(), new EditPayeeRequest("New name"));

    assertThat(response.nickname()).isEqualTo("New name");
  }

  @Test
  void removePayee_throwsNotFound_whenNotOwnedByCaller() {
    when(payeeRepository.findByIdAndOwnerUserId(any(), any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> payeeService.removePayee(CALLER, UUID.randomUUID())).isInstanceOf(
      EntityNotFoundException.class
    );
  }
}
