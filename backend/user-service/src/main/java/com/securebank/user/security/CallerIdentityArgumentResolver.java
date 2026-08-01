package com.securebank.user.security;

import com.securebank.user.config.UserServiceProperties;
import com.securebank.user.entity.UserProfile;
import com.securebank.user.enums.Role;
import com.securebank.user.repository.UserProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves the {@link CallerIdentity} for a request from the {@code X-User-Id} / {@code X-User-Role}
 * headers.
 *
 * <p>Per the zero-trust boundary in the blueprint (§3.3, NFR-S3) these headers are only meaningful
 * because the API Gateway is the sole ingress: it authenticates the session, strips any
 * client-supplied copies, and re-stamps them over mTLS. This resolver is the seam that the
 * gateway-issued JWT verification will plug into; nothing downstream reads the raw headers.
 */
@Component
@RequiredArgsConstructor
public class CallerIdentityArgumentResolver implements HandlerMethodArgumentResolver {

  public static final String USER_ID_HEADER = "X-User-Id";
  public static final String USER_ROLE_HEADER = "X-User-Role";

  private final UserProfileRepository userProfileRepository;
  private final UserServiceProperties properties;

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
    String rawUserId = request == null ? null : request.getHeader(USER_ID_HEADER);
    String rawRole = request == null ? null : request.getHeader(USER_ROLE_HEADER);

    if (rawUserId == null || rawUserId.isBlank()) {
      return demoCaller(rawRole);
    }

    UUID userId;
    try {
      userId = UUID.fromString(rawUserId.trim());
    } catch (IllegalArgumentException exception) {
      throw new AccessDeniedException(USER_ID_HEADER + " is not a valid identifier");
    }
    return new CallerIdentity(userId, parseRole(rawRole, Role.CUSTOMER));
  }

  /**
   * Development fallback so the web prototype works before auth-service issues tokens. Disabled by
   * setting {@code securebank.user.security.allow-unauthenticated-demo-caller: false}.
   */
  private CallerIdentity demoCaller(String rawRole) {
    if (!properties.security().allowUnauthenticatedDemoCaller()) {
      throw new AccessDeniedException("Missing " + USER_ID_HEADER + " identity header");
    }
    UserProfile demoUser = userProfileRepository
      .findFirstByOrderByCreatedAtAsc()
      .orElseThrow(() -> new AccessDeniedException("No demo user is available to impersonate"));
    return new CallerIdentity(demoUser.getId(), demoUser.getRole());
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
