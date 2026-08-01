package com.securebank.transfer.config;

import com.securebank.transfer.repository.PendingPayeeAdditionRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Drops unconfirmed payee-addition challenges once they expire so the table does not grow without
 * bound and no stale OTP digest is retained longer than it is useful.
 */
@Component
@RequiredArgsConstructor
public class PendingPayeeAdditionJanitor {

  private static final Logger log = LoggerFactory.getLogger(PendingPayeeAdditionJanitor.class);

  private final PendingPayeeAdditionRepository pendingPayeeAdditionRepository;

  @Scheduled(fixedDelayString = "PT15M", initialDelayString = "PT1M")
  @Transactional
  public void purgeExpiredChallenges() {
    int removed = pendingPayeeAdditionRepository.deleteUnconfirmedExpiredBefore(Instant.now());
    if (removed > 0) {
      log.debug("Purged {} expired pending payee addition(s)", removed);
    }
  }
}
