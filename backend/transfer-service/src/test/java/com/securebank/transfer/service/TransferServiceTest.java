package com.securebank.transfer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.securebank.transfer.client.AccountSnapshot;
import com.securebank.transfer.client.AccountsClient;
import com.securebank.transfer.config.TransferServiceProperties;
import com.securebank.transfer.dto.TransferQuoteRequest;
import com.securebank.transfer.dto.TransferResponse;
import com.securebank.transfer.entity.Transfer;
import com.securebank.transfer.entity.TransferDailyUsage;
import com.securebank.transfer.enums.TransferStatus;
import com.securebank.transfer.messaging.TransferEventPublisher;
import com.securebank.transfer.repository.TransferDailyUsageRepository;
import com.securebank.transfer.repository.TransferRepository;
import com.securebank.transfer.security.CallerIdentity;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final String FROM_ACCOUNT = "acc-demo-primary";
  private static final String TO_ACCOUNT = "acc-other";
  private static final CallerIdentity CALLER = new CallerIdentity(USER_ID);

  @Mock
  private TransferRepository transferRepository;

  @Mock
  private TransferDailyUsageRepository dailyUsageRepository;

  @Mock
  private AccountsClient accountsClient;

  @Mock
  private TransferEventPublisher eventPublisher;

  private TransferService transferService;

  @BeforeEach
  void setUp() {
    TransferServiceProperties properties = new TransferServiceProperties(
      null,
      new TransferServiceProperties.Limits(new BigDecimal("1000"), new BigDecimal("1500")),
      null,
      null
    );
    transferService = new TransferService(
      transferRepository,
      dailyUsageRepository,
      accountsClient,
      properties,
      eventPublisher
    );
  }

  private TransferQuoteRequest request(BigDecimal amount) {
    return new TransferQuoteRequest(FROM_ACCOUNT, TO_ACCOUNT, amount, "rent");
  }

  private Transfer.TransferBuilder pendingTransfer(BigDecimal amount) {
    return Transfer.builder()
      .id(UUID.randomUUID())
      .initiatedByUserId(USER_ID)
      .fromAccountId(FROM_ACCOUNT)
      .toAccount(TO_ACCOUNT)
      .amount(amount)
      .fee(BigDecimal.ZERO)
      .currency("LKR")
      .status(TransferStatus.PENDING_CONFIRMATION);
  }

  @Test
  void quote_rejectsTransferToSameAccount() {
    assertThatThrownBy(() ->
      transferService.quote(CALLER, new TransferQuoteRequest(FROM_ACCOUNT, FROM_ACCOUNT, BigDecimal.TEN, null), null)
    ).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void quote_rejectsAmountAboveThePerTransactionLimit() {
    assertThatThrownBy(() -> transferService.quote(CALLER, request(new BigDecimal("1500")), null))
      .isInstanceOf(LimitExceededException.class);
  }

  @Test
  void quote_rejectsWhenBalanceIsInsufficient() {
    when(accountsClient.getAccount(FROM_ACCOUNT)).thenReturn(
      new AccountSnapshot(FROM_ACCOUNT, new BigDecimal("50.00"), "LKR")
    );

    assertThatThrownBy(() -> transferService.quote(CALLER, request(new BigDecimal("100")), null))
      .isInstanceOf(InsufficientFundsException.class);
  }

  @Test
  void quote_rejectsWhenDailyLimitWouldBeExceeded() {
    when(accountsClient.getAccount(FROM_ACCOUNT)).thenReturn(
      new AccountSnapshot(FROM_ACCOUNT, new BigDecimal("10000"), "LKR")
    );
    when(dailyUsageRepository.findById(new TransferDailyUsage.Key(FROM_ACCOUNT, LocalDate.now())))
      .thenReturn(
        Optional.of(
          TransferDailyUsage.builder()
            .accountId(FROM_ACCOUNT)
            .usageDate(LocalDate.now())
            .totalAmount(new BigDecimal("1000"))
            .build()
        )
      );

    assertThatThrownBy(() -> transferService.quote(CALLER, request(new BigDecimal("900")), null))
      .isInstanceOf(LimitExceededException.class);
  }

  @Test
  void quote_returnsExistingTransfer_whenIdempotencyKeyAlreadyUsed() {
    Transfer existing = pendingTransfer(new BigDecimal("100")).build();
    when(transferRepository.findByInitiatedByUserIdAndIdempotencyKey(USER_ID, "key-1"))
      .thenReturn(Optional.of(existing));

    TransferResponse response = transferService.quote(CALLER, request(new BigDecimal("100")), "key-1");

    assertThat(response.id()).isEqualTo(existing.getId());
    verify(accountsClient, never()).getAccount(any());
  }

  @Test
  void quote_savesPendingTransfer_whenValid() {
    when(accountsClient.getAccount(FROM_ACCOUNT)).thenReturn(
      new AccountSnapshot(FROM_ACCOUNT, new BigDecimal("10000"), "LKR")
    );
    when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

    TransferResponse response = transferService.quote(CALLER, request(new BigDecimal("100")), null);

    assertThat(response.status()).isEqualTo(TransferStatus.PENDING_CONFIRMATION);
    assertThat(response.amount()).isEqualByComparingTo("100");
    assertThat(response.currency()).isEqualTo("LKR");
  }

  @Test
  void confirm_throwsNotFound_whenTransferDoesNotBelongToCaller() {
    when(transferRepository.findByIdAndInitiatedByUserId(any(), any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> transferService.confirm(CALLER, UUID.randomUUID()))
      .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void confirm_isIdempotent_whenAlreadyCompleted() {
    Transfer completed = pendingTransfer(new BigDecimal("100")).status(TransferStatus.COMPLETED).build();
    when(transferRepository.findByIdAndInitiatedByUserId(completed.getId(), USER_ID))
      .thenReturn(Optional.of(completed));

    TransferResponse response = transferService.confirm(CALLER, completed.getId());

    assertThat(response.status()).isEqualTo(TransferStatus.COMPLETED);
    verify(dailyUsageRepository, never()).findForUpdate(any(), any());
    verify(eventPublisher, never()).publishCompleted(any());
  }

  @Test
  void confirm_rejectsReconfirmingAFailedTransfer() {
    Transfer failed = pendingTransfer(new BigDecimal("100")).status(TransferStatus.FAILED).build();
    when(transferRepository.findByIdAndInitiatedByUserId(failed.getId(), USER_ID))
      .thenReturn(Optional.of(failed));

    assertThatThrownBy(() -> transferService.confirm(CALLER, failed.getId()))
      .isInstanceOf(InvalidTransferStateException.class);
  }

  @Test
  void confirm_marksFailed_whenBalanceDroppedBelowAmountSinceQuote() {
    Transfer pending = pendingTransfer(new BigDecimal("100")).build();
    when(transferRepository.findByIdAndInitiatedByUserId(pending.getId(), USER_ID))
      .thenReturn(Optional.of(pending));
    when(accountsClient.getAccount(FROM_ACCOUNT)).thenReturn(
      new AccountSnapshot(FROM_ACCOUNT, new BigDecimal("10"), "LKR")
    );
    when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

    TransferResponse response = transferService.confirm(CALLER, pending.getId());

    assertThat(response.status()).isEqualTo(TransferStatus.FAILED);
    verify(eventPublisher).publishFailed(any(), any());
    verify(dailyUsageRepository, never()).findForUpdate(any(), any());
  }

  @Test
  void confirm_updatesDailyUsageAndPublishesEvent_whenSuccessful() {
    Transfer pending = pendingTransfer(new BigDecimal("100")).build();
    when(transferRepository.findByIdAndInitiatedByUserId(pending.getId(), USER_ID))
      .thenReturn(Optional.of(pending));
    when(accountsClient.getAccount(FROM_ACCOUNT)).thenReturn(
      new AccountSnapshot(FROM_ACCOUNT, new BigDecimal("10000"), "LKR")
    );
    when(dailyUsageRepository.findForUpdate(FROM_ACCOUNT, LocalDate.now())).thenReturn(Optional.empty());
    when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

    TransferResponse response = transferService.confirm(CALLER, pending.getId());

    assertThat(response.status()).isEqualTo(TransferStatus.COMPLETED);
    verify(dailyUsageRepository).save(
      argThatUsageEquals(new BigDecimal("100"))
    );
    verify(eventPublisher).publishCompleted(any());
  }

  private TransferDailyUsage argThatUsageEquals(BigDecimal expected) {
    return org.mockito.ArgumentMatchers.argThat(usage -> usage.getTotalAmount().compareTo(expected) == 0);
  }
}
