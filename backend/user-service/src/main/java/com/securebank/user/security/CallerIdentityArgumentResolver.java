package com.securebank.user.security;

import com.securebank.user.config.UserServiceProperties;
import com.securebank.user.entity.UserProfile;
import com.securebank.user.enums.Role;
import com.securebank.user.repository.UserProfileRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves the {@link CallerIdentity} from the caller's bearer access token.
 *
 * <p>The API Gateway still forwards {@code X-User-Id} / {@code X-User-Role} for downstream
 * convenience, but this resolver treats them only as consistency checks against the verified token
 * so direct access to the service cannot impersonate another user by spoofing headers.
 */
@Component
public class CallerIdentityArgumentResolver implements HandlerMethodArgumentResolver {

  private static final String ACCESS_TOKEN_TYPE = "ACCESS";
  private static final String BEARER_PREFIX = "Bearer ";

  public static final String USER_ID_HEADER = "X-User-Id";
  public static final String USER_ROLE_HEADER = "X-User-Role";

  private final UserProfileRepository userProfileRepository;
  private final UserServiceProperties properties;
  private final SecretKey key;

  public CallerIdentityArgumentResolver(
    UserProfileRepository userProfileRepository,
    UserServiceProperties properties,
    @Value("${jwt.secret}") String secret
  ) {
    this.userProfileRepository = userProfileRepository;
    this.properties = properties;
    this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
  }

  @Override
  public boolean supportsParameter(@NonNull MethodParameter parameter) {
    return CallerIdentity.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
    @NonNull MethodParameter parameter,
    @Nullable ModelAndViewContainer mavContainer,
    @NonNull NativeWebRequest webRequest,
    @Nullable WebDataBinderFactory binderFactory
  ) {
    HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
    if (request == null) {
      return demoCaller(null);
    }

    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization != null && !authorization.isBlank()) {
      return identityFromAccessToken(authorization, request);
    }

    String rawUserId = request.getHeader(USER_ID_HEADER);
    String rawRole = request.getHeader(USER_ROLE_HEADER);
    if ((rawUserId != null && !rawUserId.isBlank()) || (rawRole != null && !rawRole.isBlank())) {
      throw new AccessDeniedException("Missing Bearer access token");
    }

    return demoCaller(rawRole);
  }

  /**
   * Development fallback so the web prototype works without auth-service issued tokens. Disabled
   * by setting {@code securebank.user.security.allow-unauthenticated-demo-caller: false}.
   */
  private CallerIdentity demoCaller(String rawRole) {
    if (!properties.security().allowUnauthenticatedDemoCaller()) {
      throw new AccessDeniedException("Missing Bearer access token");
    }
    UserProfile demoUser = userProfileRepository
      .findFirstByOrderByCreatedAtAsc()
      .orElseThrow(() -> new AccessDeniedException("No demo user is available to impersonate"));
    return new CallerIdentity(demoUser.getId(), parseRole(rawRole, demoUser.getRole()));
  }

  private CallerIdentity identityFromAccessToken(String authorization, HttpServletRequest request) {
    if (!authorization.startsWith(BEARER_PREFIX)) {
      throw new AccessDeniedException("Missing Bearer access token");
    }

    String token = authorization.substring(BEARER_PREFIX.length()).trim();
    if (token.isEmpty()) {
      throw new AccessDeniedException("Missing Bearer access token");
    }

    try {
      Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      String tokenType = claims.get("type", String.class);
      if (!ACCESS_TOKEN_TYPE.equals(tokenType)) {
        throw new AccessDeniedException("Only access tokens can call user-service");
      }

      UUID userId = parseUserId(claims.getSubject());
      Role role = parseRole(claims.get("role", String.class), Role.CUSTOMER);
      validateHeaderConsistency(request, userId, role);
      return new CallerIdentity(userId, role);
    } catch (JwtException | IllegalArgumentException exception) {
      throw new AccessDeniedException("Invalid or expired access token");
    }
  }

  private void validateHeaderConsistency(HttpServletRequest request, UUID userId, Role role) {
    String rawUserId = request.getHeader(USER_ID_HEADER);
    if (rawUserId != null && !rawUserId.isBlank() && !userId.toString().equals(rawUserId.trim())) {
      throw new AccessDeniedException(USER_ID_HEADER + " does not match the access token subject");
    }

    String rawRole = request.getHeader(USER_ROLE_HEADER);
    if (rawRole != null && !rawRole.isBlank()) {
      Role headerRole = parseRole(rawRole, role);
      if (headerRole != role) {
        throw new AccessDeniedException(USER_ROLE_HEADER + " does not match the access token role");
      }
    }
  }

  private UUID parseUserId(String subject) {
    if (subject == null || subject.isBlank()) {
      throw new AccessDeniedException("Access token is missing a subject");
    }

    try {
      return UUID.fromString(subject.trim());
    } catch (IllegalArgumentException exception) {
      throw new AccessDeniedException("Access token subject is not a valid identifier");
    }
  }

  private Role parseRole(String rawRole, Role fallback) {
    if (rawRole == null || rawRole.isBlank()) {
      return fallback;
    }
    try {
      return Role.valueOf(rawRole.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      throw new AccessDeniedException(USER_ROLE_HEADER + " '" + rawRole + "' is not a known role");
    }
  }
}
