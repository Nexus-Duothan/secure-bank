package com.securebank.user.client;

import com.securebank.user.enums.Role;
import com.securebank.user.enums.UserStatus;
import java.util.UUID;

/**
 * Seam onto auth-service, which owns sign-in.
 *
 * <p>The profile row this service keeps is only what the app displays; whether someone can
 * actually log in, and as what, is decided by the credentials in auth-service. An administrative
 * role or status change has to be pushed there or it changes nothing that matters.
 */
public interface CredentialAccessClient {
  /** Either argument may be null, meaning "leave that one alone". */
  void updateAccess(UUID userId, UserStatus status, Role role);
}
