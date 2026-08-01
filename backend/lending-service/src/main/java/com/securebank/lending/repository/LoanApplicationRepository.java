package com.securebank.lending.repository;

import com.securebank.lending.entity.LoanApplication;
import com.securebank.lending.enums.ApplicationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {
  List<LoanApplication> findByApplicantUserIdOrderByCreatedAtDesc(UUID applicantUserId);

  Optional<LoanApplication> findByIdAndApplicantUserId(UUID id, UUID applicantUserId);

  List<LoanApplication> findByStatusOrderByCreatedAtAsc(ApplicationStatus status);
}
