package com.securebank.transfer.repository;

import com.securebank.transfer.entity.ScheduledTransfer;
import com.securebank.transfer.enums.ScheduleStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer, UUID> {
  List<ScheduledTransfer> findByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);

  Optional<ScheduledTransfer> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

  /** Cheap, non-locking poll: just the ids the runner needs to hand off for locked execution. */
  @Query("select s.id from ScheduledTransfer s where s.status = :status and s.nextRunAt <= :now")
  List<UUID> findDueIds(ScheduleStatus status, Instant now);

  /**
   * Locked re-read of a single due schedule before executing it, so two overlapping runner ticks
   * (or a slow previous run still in flight) can't both execute the same occurrence.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from ScheduledTransfer s where s.id = :id")
  Optional<ScheduledTransfer> findForUpdateById(UUID id);
}
