package com.securebank.user.config;

import com.securebank.user.repository.PendingUserChangeRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Drops unconfirmed challenges once they expire so the table does not grow without bound and no
 * stale OTP digest is retained longer than it is useful.
 */
@Component
@RequiredArgsConstructor
public class PendingChangeJanitor {

  private static final Logger log = LoggerFactory.getLogger(PendingChangeJanitor.class);

  private final PendingUserChangeRepository pendingUserChangeRepository;

  @Scheduled(fixedDelayString = "PT15M", initialDelayString = "PT1M")
  @Transactional
  public void purgeExpiredChallenges() {
    int removed = pendingUserChangeRepository.deleteUnconfirmedExpiredBefore(Instant.now());
    if (removed > 0) {
      log.debug("Purged {} expired pending change request(s)", removed);
    }
  }
}
