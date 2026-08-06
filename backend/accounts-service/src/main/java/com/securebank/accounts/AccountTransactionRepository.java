package com.securebank.accounts;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTransactionRepository
  extends JpaRepository<AccountTransactionEntity, String>
{
  /** The bank-wide journal, newest first, for the admin audit view. */
  List<AccountTransactionEntity> findAllByOrderByOccurredAtDesc(Pageable pageable);

  List<AccountTransactionEntity> findByAccountIdOrderByOccurredAtDesc(String accountId);

  List<AccountTransactionEntity> findByAccountIdAndOccurredAtAfter(
    String accountId,
    Instant occurredAt
  );

  Optional<AccountTransactionEntity> findByAccountIdAndReference(
    String accountId,
    String reference
  );
}
