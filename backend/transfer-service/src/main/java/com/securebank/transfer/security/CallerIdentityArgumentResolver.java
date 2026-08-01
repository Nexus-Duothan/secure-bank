package com.securebank.transfer.security;

import com.securebank.transfer.config.TransferServiceProperties;
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
 * <p>The API Gateway also forwards {@code X-User-Id} for downstream convenience, but this
 * resolver treats it only as a consistency check against the verified token so direct access to
 * the service cannot impersonate another user by spoofing headers.
 */
@Component
public class CallerIdentityArgumentResolver implements HandlerMethodArgumentResolver {

  private static final String ACCESS_TOKEN_TYPE = "ACCESS";
  private static final String BEARER_PREFIX = "Bearer ";
  public static final String USER_ID_HEADER = "X-User-Id";

  private final TransferServiceProperties properties;
  private final SecretKey key;

  public CallerIdentityArgumentResolver(
    TransferServiceProperties properties,
    @Value("${jwt.secret}") String secret
  ) {
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
      return demoCaller();
    }

    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization != null && !authorization.isBlank()) {
      return identityFromAccessToken(authorization, request);
    }

    String rawUserId = request.getHeader(USER_ID_HEADER);
    if (rawUserId != null && !rawUserId.isBlank()) {
      throw new AccessDeniedException("Missing Bearer access token");
    }

    return demoCaller();
  }

  /**
   * Development fallback so the web prototype works without auth-service issued tokens. Disabled
   * by setting {@code securebank.transfer.security.allow-unauthenticated-demo-caller: false}.
   */
  private CallerIdentity demoCaller() {
    if (!properties.security().allowUnauthenticatedDemoCaller()) {
      throw new AccessDeniedException("Missing Bearer access token");
    }
    return new CallerIdentity(properties.security().demoUserId());
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
        throw new AccessDeniedException("Only access tokens can call transfer-service");
      }

      UUID userId = parseUserId(claims.getSubject());
      validateHeaderConsistency(request, userId);
      return new CallerIdentity(userId);
    } catch (JwtException | IllegalArgumentException exception) {
      throw new AccessDeniedException("Invalid or expired access token");
    }
  }

  private void validateHeaderConsistency(HttpServletRequest request, UUID userId) {
    String rawUserId = request.getHeader(USER_ID_HEADER);
    if (rawUserId != null && !rawUserId.isBlank() && !userId.toString().equals(rawUserId.trim())) {
      throw new AccessDeniedException(USER_ID_HEADER + " does not match the access token subject");
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
}
