package com.securebank.transfer.repository;

import com.securebank.transfer.entity.TransferDailyUsage;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TransferDailyUsageRepository
  extends JpaRepository<TransferDailyUsage, TransferDailyUsage.Key>
{
  /**
   * Row-level lock so two concurrent transfers from the same account serialise on their shared
   * daily-usage counter instead of both reading a stale total and jointly blowing past the limit.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
    "select u from TransferDailyUsage u where u.accountId = :accountId and u.usageDate = :usageDate"
  )
  Optional<TransferDailyUsage> findForUpdate(String accountId, LocalDate usageDate);
}
