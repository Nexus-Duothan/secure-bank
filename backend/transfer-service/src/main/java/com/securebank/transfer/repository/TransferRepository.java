package com.securebank.transfer.repository;

import com.securebank.transfer.entity.Transfer;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {
  Optional<Transfer> findByIdAndInitiatedByUserId(UUID id, UUID initiatedByUserId);

  Optional<Transfer> findByInitiatedByUserIdAndIdempotencyKey(
    UUID initiatedByUserId,
    String idempotencyKey
  );

  /**
   * Locked read used by {@code confirm()} so two concurrent confirm calls on the same transfer
   * serialise instead of both reading PENDING_CONFIRMATION and both executing it.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from Transfer t where t.id = :id and t.initiatedByUserId = :initiatedByUserId")
  Optional<Transfer> findForUpdateByIdAndInitiatedByUserId(UUID id, UUID initiatedByUserId);
}
