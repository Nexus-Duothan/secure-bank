package com.securebank.lending.service;

import com.securebank.lending.config.LendingServiceProperties;
import com.securebank.lending.dto.LoanApplicationRequest;
import com.securebank.lending.dto.LoanApplicationResponse;
import com.securebank.lending.dto.LoanApplicationReviewRequest;
import com.securebank.lending.entity.Loan;
import com.securebank.lending.entity.LoanApplication;
import com.securebank.lending.entity.LoanInstallment;
import com.securebank.lending.enums.ApplicationStatus;
import com.securebank.lending.exception.InvalidApplicationStateException;
import com.securebank.lending.exception.LoanLimitExceededException;
import com.securebank.lending.exception.ResourceNotFoundException;
import com.securebank.lending.kafka.LoanEventProducer;
import com.securebank.lending.kafka.event.LoanDisbursedEvent;
import com.securebank.lending.repository.LoanApplicationRepository;
import com.securebank.lending.repository.LoanInstallmentRepository;
import com.securebank.lending.repository.LoanRepository;
import com.securebank.lending.util.AmortizationCalculator;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loan origination and review (FR-22, FR-23). Applications land straight in UNDER_REVIEW —
 * this is a synchronous digital intake with no separate manual "claim from queue" step, so
 * there's no meaningful window where an application is SUBMITTED but not yet queued for
 * review. Approving an application disburses it in the same transaction: creates the Loan,
 * computes and persists its full amortization schedule (FR-24), and publishes a
 * LoanDisbursedEvent.
 */
@Service
@RequiredArgsConstructor
public class LoanApplicationService {

  private final LoanApplicationRepository loanApplicationRepository;
  private final LoanRepository loanRepository;
  private final LoanInstallmentRepository loanInstallmentRepository;
  private final AmortizationCalculator amortizationCalculator;
  private final LoanEventProducer loanEventProducer;
  private final LendingServiceProperties properties;

  @Transactional
  public LoanApplicationResponse apply(UUID applicantUserId, LoanApplicationRequest request) {
    LendingServiceProperties.Loan limits = properties.loan();

    if (
      request.amount().compareTo(limits.minAmount()) < 0 ||
      request.amount().compareTo(limits.maxAmount()) > 0
    ) {
      throw new LoanLimitExceededException(
        "Amount must be between " + limits.minAmount() + " and " + limits.maxAmount()
      );
    }
    if (
      request.termMonths() < limits.minTermMonths() || request.termMonths() > limits.maxTermMonths()
    ) {
      throw new LoanLimitExceededException(
        "Term must be between " +
          limits.minTermMonths() +
          " and " +
          limits.maxTermMonths() +
          " months"
      );
    }

    LoanApplication application = loanApplicationRepository.save(
      LoanApplication.builder()
        .applicantUserId(applicantUserId)
        .purpose(request.purpose().trim())
        .amount(request.amount())
        .termMonths(request.termMonths())
        .annualInterestRate(limits.defaultAnnualInterestRate())
        .linkedAccountId(request.linkedAccountId().trim())
        .status(ApplicationStatus.UNDER_REVIEW)
        .build()
    );

    return LoanApplicationResponse.from(application);
  }

  @Transactional(readOnly = true)
  public LoanApplicationResponse get(UUID applicantUserId, UUID applicationId) {
    return LoanApplicationResponse.from(findOwned(applicantUserId, applicationId));
  }

  @Transactional(readOnly = true)
  public List<LoanApplicationResponse> list(UUID applicantUserId) {
    return loanApplicationRepository
      .findByApplicantUserIdOrderByCreatedAtDesc(applicantUserId)
      .stream()
      .map(LoanApplicationResponse::from)
      .toList();
  }

  @Transactional(readOnly = true)
  public List<LoanApplicationResponse> listPendingReview() {
    return loanApplicationRepository
      .findByStatusOrderByCreatedAtAsc(ApplicationStatus.UNDER_REVIEW)
      .stream()
      .map(LoanApplicationResponse::from)
      .toList();
  }

  @Transactional
  public LoanApplicationResponse review(
    UUID officerId,
    UUID applicationId,
    LoanApplicationReviewRequest request
  ) {
    LoanApplication application = loanApplicationRepository
      .findById(applicationId)
      .orElseThrow(() ->
        new ResourceNotFoundException("Loan application not found: " + applicationId)
      );

    if (application.getStatus() != ApplicationStatus.UNDER_REVIEW) {
      throw new InvalidApplicationStateException(
        "Application is " + application.getStatus() + " and can no longer be reviewed"
      );
    }
    // Segregation of duties: an officer must not be able to approve/reject their own
    // application (e.g. by applying for a personal loan under their own customer account).
    if (application.getApplicantUserId().equals(officerId)) {
      throw new InvalidApplicationStateException(
        "Officers cannot review their own loan applications"
      );
    }

    application.setReviewedBy(officerId);
    application.setReviewedAt(Instant.now());

    if (Boolean.TRUE.equals(request.approve())) {
      application.setStatus(ApplicationStatus.APPROVED);
      loanApplicationRepository.save(application);
      disburse(application);
    } else {
      application.setStatus(ApplicationStatus.REJECTED);
      application.setRejectionReason(
        request.note() != null ? request.note() : "Declined by reviewing officer"
      );
      loanApplicationRepository.save(application);
    }

    return LoanApplicationResponse.from(application);
  }

  private void disburse(LoanApplication application) {
    Instant now = Instant.now();
    Loan loan = loanRepository.save(
      Loan.builder()
        .applicationId(application.getId())
        .borrowerUserId(application.getApplicantUserId())
        .purpose(application.getPurpose())
        .principal(application.getAmount())
        .annualInterestRate(application.getAnnualInterestRate())
        .termMonths(application.getTermMonths())
        .linkedAccountId(application.getLinkedAccountId())
        .disbursedAt(now)
        .build()
    );

    List<LoanInstallment> schedule = amortizationCalculator.buildSchedule(
      loan.getId(),
      loan.getPrincipal(),
      loan.getAnnualInterestRate(),
      loan.getTermMonths(),
      now
    );
    loanInstallmentRepository.saveAll(schedule);

    application.setStatus(ApplicationStatus.DISBURSED);
    application.setLoanId(loan.getId());
    loanApplicationRepository.save(application);

    loanEventProducer.publishDisbursed(
      new LoanDisbursedEvent(
        loan.getId(),
        loan.getBorrowerUserId(),
        loan.getPrincipal(),
        loan.getCurrency(),
        loan.getTermMonths(),
        now
      )
    );
  }

  private LoanApplication findOwned(UUID applicantUserId, UUID applicationId) {
    return loanApplicationRepository
      .findByIdAndApplicantUserId(applicationId, applicantUserId)
      .orElseThrow(() ->
        new ResourceNotFoundException("Loan application not found: " + applicationId)
      );
  }
}
