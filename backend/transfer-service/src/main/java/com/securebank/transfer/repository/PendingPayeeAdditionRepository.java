package com.securebank.transfer.repository;

import com.securebank.transfer.entity.PendingPayeeAddition;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PendingPayeeAdditionRepository extends JpaRepository<PendingPayeeAddition, UUID> {
  /**
   * Scoped lookup: a challenge can only be confirmed by the caller that raised it, so a leaked
   * change id is useless to another caller.
   */
  Optional<PendingPayeeAddition> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

  @Modifying
  @Query("delete from PendingPayeeAddition c where c.confirmed = false and c.expiresAt < :cutoff")
  int deleteUnconfirmedExpiredBefore(Instant cutoff);
}
