package com.securebank.transfer.repository;

import com.securebank.transfer.entity.Payee;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayeeRepository extends JpaRepository<Payee, UUID> {
  List<Payee> findByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);

  Optional<Payee> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

  Optional<Payee> findByOwnerUserIdAndAccountReferenceIgnoreCase(
    UUID ownerUserId,
    String accountReference
  );

  boolean existsByOwnerUserIdAndAccountReferenceIgnoreCase(UUID ownerUserId, String accountReference);
}
