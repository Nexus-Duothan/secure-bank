package com.securebank.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.securebank.user.entity.UserProfile;
import com.securebank.user.enums.Role;
import com.securebank.user.enums.UserStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

/**
 * A profile is addressed by the id auth-service holds for the customer, which is the same id the
 * gateway forwards as {@code X-User-Id}. If the persistence layer ever minted an id of its own the
 * row would be created but unreachable, and every {@code /users/me} call would answer 404.
 */
@DataJpaTest
@TestPropertySource(
  properties = {
    "spring.datasource.url=jdbc:h2:mem:userprofiletest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
  }
)
class UserProfileRepositoryTest {

  @Autowired
  private UserProfileRepository userProfileRepository;

  @Test
  void keepsTheIdItWasGivenSoTheCallerCanBeFoundAgain() {
    UUID authUserId = UUID.randomUUID();

    UserProfile saved = userProfileRepository.save(
      UserProfile.builder()
        .id(authUserId)
        .fullName("Probe User")
        .email("probe@example.com")
        .role(Role.CUSTOMER)
        .status(UserStatus.PENDING_REVIEW)
        .build()
    );

    assertThat(saved.getId()).isEqualTo(authUserId);
    assertThat(userProfileRepository.findById(authUserId)).isPresent();
  }
}
