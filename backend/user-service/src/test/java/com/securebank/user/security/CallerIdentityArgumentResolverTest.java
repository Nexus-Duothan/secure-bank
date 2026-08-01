package com.securebank.user.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.securebank.user.config.UserServiceProperties;
import com.securebank.user.entity.UserProfile;
import com.securebank.user.enums.Role;
import com.securebank.user.enums.UserStatus;
import com.securebank.user.repository.UserProfileRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

@ExtendWith(MockitoExtension.class)
class CallerIdentityArgumentResolverTest {

  private static final String JWT_SECRET =
    "dGhpc0lzQVZlcnlTZWN1cmVTZWNyZXRLZXlGb3JTZWN1cmVCYW5rSkdUVG9rZW5zMjAyNiE=";

  @Mock
  private UserProfileRepository userProfileRepository;

  @Mock
  private MethodParameter methodParameter;

  private CallerIdentityArgumentResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new CallerIdentityArgumentResolver(
      userProfileRepository,
      properties(false, false),
      JWT_SECRET
    );
  }

  @Test
  void resolvesCallerFromAccessToken() {
    UUID userId = UUID.randomUUID();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(userId, Role.ADMIN));

    CallerIdentity caller = (CallerIdentity) resolver.resolveArgument(
      methodParameter,
      null,
      new ServletWebRequest(request),
      null
    );

    assertThat(caller.userId()).isEqualTo(userId);
    assertThat(caller.role()).isEqualTo(Role.ADMIN);
  }

  @Test
  void rejectsSpoofedHeadersThatDoNotMatchToken() {
    UUID userId = UUID.randomUUID();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(userId, Role.CUSTOMER));
    request.addHeader(CallerIdentityArgumentResolver.USER_ID_HEADER, UUID.randomUUID().toString());

    assertThatThrownBy(() ->
      resolver.resolveArgument(methodParameter, null, new ServletWebRequest(request), null)
    )
      .isInstanceOf(AccessDeniedException.class)
      .hasMessageContaining("does not match");
  }

  @Test
  void rejectsDirectHeaderOnlyCallsWhenDemoFallbackIsDisabled() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CallerIdentityArgumentResolver.USER_ID_HEADER, UUID.randomUUID().toString());
    request.addHeader(CallerIdentityArgumentResolver.USER_ROLE_HEADER, Role.ADMIN.name());

    assertThatThrownBy(() ->
      resolver.resolveArgument(methodParameter, null, new ServletWebRequest(request), null)
    )
      .isInstanceOf(AccessDeniedException.class)
      .hasMessage("Missing Bearer access token");
  }

  @Test
  void fallsBackToDemoCallerOnlyWhenExplicitlyEnabled() {
    UUID demoUserId = UUID.randomUUID();
    UserProfile demoUser = UserProfile.builder()
      .id(demoUserId)
      .fullName("John A. Doe")
      .email("john.doe@securebank.lk")
      .phoneNumber("+94 77 510 6101")
      .addressLine("42 Lake Drive")
      .city("Kandy")
      .country("Sri Lanka")
      .language("English")
      .role(Role.CUSTOMER)
      .status(UserStatus.ACTIVE)
      .createdAt(Instant.now())
      .updatedAt(Instant.now())
      .build();

    when(userProfileRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(demoUser));

    CallerIdentityArgumentResolver demoResolver = new CallerIdentityArgumentResolver(
      userProfileRepository,
      properties(true, true),
      JWT_SECRET
    );

    CallerIdentity caller = (CallerIdentity) demoResolver.resolveArgument(
      methodParameter,
      null,
      new ServletWebRequest(new MockHttpServletRequest()),
      null
    );

    assertThat(caller.userId()).isEqualTo(demoUserId);
    assertThat(caller.role()).isEqualTo(Role.CUSTOMER);
  }

  private UserServiceProperties properties(boolean seedDemoData, boolean allowDemoCaller) {
    return new UserServiceProperties(
      seedDemoData,
      new UserServiceProperties.Cors(List.of("http://localhost:5173")),
      new UserServiceProperties.Otp(null, 5, false),
      new UserServiceProperties.Security(allowDemoCaller)
    );
  }

  private String accessToken(UUID userId, Role role) {
    SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));
    Instant now = Instant.now();
    return Jwts.builder()
      .subject(userId.toString())
      .claim("username", "john.doe")
      .claim("role", role.name())
      .claim("type", "ACCESS")
      .issuedAt(Date.from(now))
      .expiration(Date.from(now.plusSeconds(300)))
      .signWith(key)
      .compact();
  }
}
