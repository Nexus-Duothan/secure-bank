package com.securebank.user.security;

import com.securebank.user.enums.Role;
import java.util.Arrays;
import java.util.UUID;

/**
 * The authenticated principal behind the current request, resolved from the identity headers the
 * API Gateway stamps on every proxied call.
 *
 * <p>Controllers declare this as a method parameter; {@link CallerIdentityArgumentResolver}
 * populates it.
 */
public record CallerIdentity(UUID userId, Role role) {
  public boolean hasAnyRole(Role... allowed) {
    return Arrays.asList(allowed).contains(role);
  }

  public void requireAnyRole(Role... allowed) {
    if (!hasAnyRole(allowed)) {
      throw new AccessDeniedException(
        "Role " + role + " is not permitted to perform this operation"
      );
    }
  }

  public boolean is(UUID otherUserId) {
    return userId.equals(otherUserId);
  }
}
