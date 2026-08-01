package com.securebank.lending.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.securebank.lending.client.AccountSnapshot;
import com.securebank.lending.client.AccountsClient;
import com.securebank.lending.config.LendingServiceProperties;
import com.securebank.lending.entity.Loan;
import com.securebank.lending.entity.LoanInstallment;
import com.securebank.lending.enums.InstallmentStatus;
import com.securebank.lending.enums.LoanStatus;
import com.securebank.lending.kafka.LoanEventProducer;
import com.securebank.lending.repository.LoanInstallmentRepository;
import com.securebank.lending.repository.LoanRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstallmentExecutionServiceTest {

  private static final UUID LOAN_ID = UUID.randomUUID();
  private static final String ACCOUNT_ID = "acc-demo-primary";

  @Mock
  private LoanRepository loanRepository;

  @Mock
  private LoanInstallmentRepository loanInstallmentRepository;

  @Mock
  private AccountsClient accountsClient;

  @Mock
  private LoanEventProducer loanEventProducer;

  private InstallmentExecutionService service;

  @BeforeEach
  void setUp() {
    LendingServiceProperties properties = new LendingServiceProperties(
      null,
      null,
      new LendingServiceProperties.Repayment(3, Duration.ofDays(1), Duration.ofDays(3))
    );
    service = new InstallmentExecutionService(
      loanRepository,
      loanInstallmentRepository,
      accountsClient,
      properties,
      loanEventProducer
    );
  }

  private Loan loan() {
    return Loan.builder()
      .id(LOAN_ID)
      .borrowerUserId(UUID.randomUUID())
      .linkedAccountId(ACCOUNT_ID)
      .status(LoanStatus.ACTIVE)
      .build();
  }

  private LoanInstallment.LoanInstallmentBuilder installment() {
    return LoanInstallment.builder()
      .id(UUID.randomUUID())
      .loanId(LOAN_ID)
      .installmentNumber(1)
      .dueDate(Instant.now())
      .totalAmount(new BigDecimal("1000"))
      .status(InstallmentStatus.PENDING)
      .failedAttempts(0);
  }

  @Test
  void collectDue_marksPaid_whenBalanceIsSufficient() {
    LoanInstallment installment = installment().build();
    when(loanInstallmentRepository.findForUpdateById(installment.getId())).thenReturn(
      Optional.of(installment)
    );
    when(loanRepository.findForUpdateById(LOAN_ID)).thenReturn(Optional.of(loan()));
    when(accountsClient.getAccount(ACCOUNT_ID)).thenReturn(
      new AccountSnapshot(ACCOUNT_ID, new BigDecimal("5000"), "LKR")
    );
    when(
      loanInstallmentRepository.findFirstByLoanIdAndStatusInOrderByInstallmentNumberAsc(
        any(),
        any()
      )
    ).thenReturn(Optional.empty());

    LoanInstallment result = service.collectDue(installment.getId());

    assertThat(result.getStatus()).isEqualTo(InstallmentStatus.PAID);
    assertThat(result.getPaidAt()).isNotNull();
  }

  @Test
  void collectDue_marksLoanPaidOff_whenNoInstallmentsRemainOutstanding() {
    LoanInstallment installment = installment().build();
    Loan loan = loan();
    when(loanInstallmentRepository.findForUpdateById(installment.getId())).thenReturn(
      Optional.of(installment)
    );
    when(loanRepository.findForUpdateById(LOAN_ID)).thenReturn(Optional.of(loan));
    when(accountsClient.getAccount(ACCOUNT_ID)).thenReturn(
      new AccountSnapshot(ACCOUNT_ID, new BigDecimal("5000"), "LKR")
    );
    when(
      loanInstallmentRepository.findFirstByLoanIdAndStatusInOrderByInstallmentNumberAsc(
        any(),
        any()
      )
    ).thenReturn(Optional.empty());

    service.collectDue(installment.getId());

    assertThat(loan.getStatus()).isEqualTo(LoanStatus.PAID_OFF);
  }

  @Test
  void collectDue_recordsFailedAttempt_whenBalanceIsInsufficient() {
    LoanInstallment installment = installment().build();
    when(loanInstallmentRepository.findForUpdateById(installment.getId())).thenReturn(
      Optional.of(installment)
    );
    when(loanRepository.findForUpdateById(LOAN_ID)).thenReturn(Optional.of(loan()));
    when(accountsClient.getAccount(ACCOUNT_ID)).thenReturn(
      new AccountSnapshot(ACCOUNT_ID, new BigDecimal("10"), "LKR")
    );

    LoanInstallment result = service.collectDue(installment.getId());

    assertThat(result.getStatus()).isEqualTo(InstallmentStatus.FAILED);
    assertThat(result.getFailedAttempts()).isEqualTo(1);
    assertThat(result.getNextRetryAt()).isNotNull();
    verify(loanEventProducer, never()).publishOverdue(any());
  }

  @Test
  void collectDue_marksOverdueAndDelinquent_whenRetryAttemptsExhausted() {
    LoanInstallment installment = installment().failedAttempts(2).build();
    Loan loan = loan();
    when(loanInstallmentRepository.findForUpdateById(installment.getId())).thenReturn(
      Optional.of(installment)
    );
    when(loanRepository.findForUpdateById(LOAN_ID)).thenReturn(Optional.of(loan));
    when(accountsClient.getAccount(ACCOUNT_ID)).thenReturn(
      new AccountSnapshot(ACCOUNT_ID, new BigDecimal("10"), "LKR")
    );

    LoanInstallment result = service.collectDue(installment.getId());

    assertThat(result.getStatus()).isEqualTo(InstallmentStatus.OVERDUE);
    assertThat(loan.getStatus()).isEqualTo(LoanStatus.DELINQUENT);
    verify(loanEventProducer).publishOverdue(any());
  }

  @Test
  void collectDue_isIdempotent_whenAlreadyPaid() {
    LoanInstallment installment = installment().status(InstallmentStatus.PAID).build();
    when(loanInstallmentRepository.findForUpdateById(installment.getId())).thenReturn(
      Optional.of(installment)
    );

    service.collectDue(installment.getId());

    verify(loanRepository, never()).findForUpdateById(any());
    verify(accountsClient, never()).getAccount(any());
  }
}
