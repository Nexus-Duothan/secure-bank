package com.securebank.lending.repository;

import com.securebank.lending.entity.LoanInstallment;
import com.securebank.lending.enums.InstallmentStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, UUID> {
  List<LoanInstallment> findByLoanIdOrderByInstallmentNumberAsc(UUID loanId);

  Optional<LoanInstallment> findFirstByLoanIdAndStatusInOrderByInstallmentNumberAsc(
    UUID loanId,
    List<InstallmentStatus> statuses
  );

  /**
   * Cheap, non-locking poll: due PENDING installments, or FAILED ones whose retry is now due,
   * for loans that still have autopay enabled (a paused loan's installments still appear in
   * the schedule and reminders, but the scheduled runner must not collect them - only the
   * manual "pay now" action does).
   */
  @Query(
    "select i.id from LoanInstallment i, Loan l where i.loanId = l.id and l.autopayEnabled = true and (" +
      "(i.status = com.securebank.lending.enums.InstallmentStatus.PENDING and i.dueDate <= :now) or " +
      "(i.status = com.securebank.lending.enums.InstallmentStatus.FAILED and i.nextRetryAt <= :now))"
  )
  List<UUID> findDueForCollectionIds(Instant now);

  /** Cheap, non-locking poll: PENDING installments due within the reminder lead time, not yet reminded. */
  @Query(
    "select i.id from LoanInstallment i where i.status = com.securebank.lending.enums.InstallmentStatus.PENDING " +
      "and i.reminderSentAt is null and i.dueDate <= :remindBefore"
  )
  List<UUID> findDueForReminderIds(Instant remindBefore);

  /** Locked re-read before executing a collection against this installment. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from LoanInstallment i where i.id = :id")
  Optional<LoanInstallment> findForUpdateById(UUID id);
}
