package com.securebank.transfer.security;

import java.util.UUID;

/**
 * The authenticated principal behind the current request, resolved from the caller's verified
 * access token (see {@link CallerIdentityArgumentResolver}).
 */
public record CallerIdentity(UUID userId) {
  public boolean is(UUID otherUserId) {
    return userId.equals(otherUserId);
  }
}
