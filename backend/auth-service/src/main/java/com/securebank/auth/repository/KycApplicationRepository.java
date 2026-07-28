package com.securebank.auth.repository;

import com.securebank.auth.entity.KycApplication;
import com.securebank.auth.enums.KycStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KycApplicationRepository extends JpaRepository<KycApplication, UUID> {
  List<KycApplication> findByStatus(KycStatus status);
  Optional<KycApplication> findByUserId(UUID userId);
}
