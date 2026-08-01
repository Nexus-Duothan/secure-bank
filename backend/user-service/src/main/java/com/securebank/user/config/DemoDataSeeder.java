package com.securebank.user.config;

import com.securebank.user.entity.UserDevice;
import com.securebank.user.entity.UserProfile;
import com.securebank.user.enums.Role;
import com.securebank.user.enums.UserStatus;
import com.securebank.user.repository.UserProfileRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the walkthrough accounts used by the web prototype.
 *
 * <p>Runs as an {@link ApplicationRunner} rather than from {@code @PostConstruct}: Spring applies
 * {@code @Transactional} through a proxy that is not yet in place during bean initialisation, so a
 * transactional post-construct hook runs with no transaction at all.
 *
 * <p>Two profiles are created because role administration (FR-08) is only demonstrable when an
 * administrator and a subject customer both exist. The customer is written first so it remains the
 * profile that the unauthenticated development fallback resolves to.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "securebank.user", name = "seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

  private final UserProfileRepository userProfileRepository;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (userProfileRepository.count() > 0) {
      return;
    }

    UserProfile customer = UserProfile.builder()
      .fullName("John Doe")
      .email("john.doe@securebank.lk")
      .phoneNumber("+94 77 123 4567")
      .addressLine("42 Lake Drive")
      .city("Colombo")
      .country("Sri Lanka")
      .language("English")
      .role(Role.CUSTOMER)
      .status(UserStatus.ACTIVE)
      .idVerified(true)
      .build();

    customer.addDevice(
      UserDevice.builder()
        .deviceName("Chrome on Windows")
        .deviceType("Desktop")
        .browser("Chrome")
        .location("Colombo, LK")
        .trusted(true)
        .lastVerifiedAt(Instant.now().minus(2, ChronoUnit.MINUTES))
        .build()
    );
    customer.addDevice(
      UserDevice.builder()
        .deviceName("SecureBank iOS")
        .deviceType("Mobile")
        .browser("Mobile App")
        .location("Colombo, LK")
        .trusted(true)
        .lastVerifiedAt(Instant.now().minus(3, ChronoUnit.DAYS))
        .build()
    );
    userProfileRepository.save(customer);

    UserProfile administrator = UserProfile.builder()
      .fullName("Nimali Perera")
      .email("nimali.perera@securebank.lk")
      .phoneNumber("+94 77 900 1122")
      .addressLine("SecureBank Operations Centre")
      .city("Colombo")
      .country("Sri Lanka")
      .language("English")
      .role(Role.ADMIN)
      .status(UserStatus.ACTIVE)
      .idVerified(true)
      .build();
    userProfileRepository.save(administrator);

    log.info(
      "Seeded demo profiles: customer={} administrator={}",
      customer.getId(),
      administrator.getId()
    );
  }
}
