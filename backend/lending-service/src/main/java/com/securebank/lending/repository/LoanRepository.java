package com.securebank.lending.repository;

import com.securebank.lending.entity.Loan;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface LoanRepository extends JpaRepository<Loan, UUID> {
  List<Loan> findByBorrowerUserIdOrderByDisbursedAtDesc(UUID borrowerUserId);

  Optional<Loan> findByIdAndBorrowerUserId(UUID id, UUID borrowerUserId);

  /** Locked re-read before executing a collection against this loan (manual pay or auto-deduct). */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select l from Loan l where l.id = :id")
  Optional<Loan> findForUpdateById(UUID id);
}
