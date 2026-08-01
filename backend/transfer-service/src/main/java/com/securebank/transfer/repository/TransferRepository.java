package com.securebank.transfer.repository;

import com.securebank.transfer.entity.Transfer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {
  Optional<Transfer> findByIdAndInitiatedByUserId(UUID id, UUID initiatedByUserId);

  Optional<Transfer> findByInitiatedByUserIdAndIdempotencyKey(
    UUID initiatedByUserId,
    String idempotencyKey
  );
}
