package com.securebank.lending.service;

import com.securebank.lending.dto.LoanInstallmentResponse;
import com.securebank.lending.dto.LoanResponse;
import com.securebank.lending.entity.Loan;
import com.securebank.lending.entity.LoanInstallment;
import com.securebank.lending.enums.InstallmentStatus;
import com.securebank.lending.exception.ResourceNotFoundException;
import com.securebank.lending.repository.LoanInstallmentRepository;
import com.securebank.lending.repository.LoanRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-24: repayment schedule display, plus the borrower-facing "pay now" and autopay toggle actions. */
@Service
@RequiredArgsConstructor
public class LoanService {

  private final LoanRepository loanRepository;
  private final LoanInstallmentRepository loanInstallmentRepository;
  private final InstallmentExecutionService installmentExecutionService;

  @Transactional(readOnly = true)
  public LoanResponse get(UUID borrowerUserId, UUID loanId) {
    return toResponse(findOwned(borrowerUserId, loanId));
  }

  @Transactional(readOnly = true)
  public List<LoanResponse> list(UUID borrowerUserId) {
    return loanRepository
      .findByBorrowerUserIdOrderByDisbursedAtDesc(borrowerUserId)
      .stream()
      .map(this::toResponse)
      .toList();
  }

  @Transactional(readOnly = true)
  public List<LoanInstallmentResponse> listInstallments(UUID borrowerUserId, UUID loanId) {
    Loan loan = findOwned(borrowerUserId, loanId);
    return loanInstallmentRepository
      .findByLoanIdOrderByInstallmentNumberAsc(loan.getId())
      .stream()
      .map(LoanInstallmentResponse::from)
      .toList();
  }

  @Transactional
  public LoanResponse updateAutopay(UUID borrowerUserId, UUID loanId, boolean enabled) {
    Loan loan = findOwned(borrowerUserId, loanId);
    loan.setAutopayEnabled(enabled);
    return toResponse(loanRepository.save(loan));
  }

  /** Manually collects the loan's earliest outstanding installment, ahead of its due date if need be. */
  @Transactional
  public LoanResponse payNow(UUID borrowerUserId, UUID loanId) {
    Loan loan = findOwned(borrowerUserId, loanId);

    LoanInstallment nextOutstanding = loanInstallmentRepository
      .findFirstByLoanIdAndStatusInOrderByInstallmentNumberAsc(
        loan.getId(),
        List.of(InstallmentStatus.PENDING, InstallmentStatus.FAILED, InstallmentStatus.OVERDUE)
      )
      .orElseThrow(() -> new IllegalStateException("This loan has no outstanding installments"));

    installmentExecutionService.collectDue(nextOutstanding.getId());

    return toResponse(findOwned(borrowerUserId, loanId));
  }

  private Loan findOwned(UUID borrowerUserId, UUID loanId) {
    return loanRepository
      .findByIdAndBorrowerUserId(loanId, borrowerUserId)
      .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));
  }

  private LoanResponse toResponse(Loan loan) {
    List<LoanInstallment> installments =
      loanInstallmentRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId());

    int paidCount = (int) installments
      .stream()
      .filter(installment -> installment.getStatus() == InstallmentStatus.PAID)
      .count();

    BigDecimal remainingBalance = installments
      .stream()
      .filter(installment -> installment.getStatus() == InstallmentStatus.PAID)
      .reduce((first, second) -> second)
      .map(LoanInstallment::getRemainingBalanceAfter)
      .orElse(loan.getPrincipal());

    Optional<LoanInstallment> nextOutstanding = installments
      .stream()
      .filter(installment -> installment.getStatus() != InstallmentStatus.PAID)
      .findFirst();

    return LoanResponse.builder()
      .id(loan.getId())
      .applicationId(loan.getApplicationId())
      .purpose(loan.getPurpose())
      .principal(loan.getPrincipal())
      .annualInterestRate(loan.getAnnualInterestRate())
      .termMonths(loan.getTermMonths())
      .currency(loan.getCurrency())
      .linkedAccountId(loan.getLinkedAccountId())
      .status(loan.getStatus())
      .autopayEnabled(loan.isAutopayEnabled())
      .remainingBalance(remainingBalance)
      .installmentsPaid(paidCount)
      .installmentsTotal(installments.size())
      .nextInstallmentDueDate(nextOutstanding.map(LoanInstallment::getDueDate).orElse(null))
      .nextInstallmentAmount(nextOutstanding.map(LoanInstallment::getTotalAmount).orElse(null))
      .disbursedAt(loan.getDisbursedAt())
      .build();
  }
}
