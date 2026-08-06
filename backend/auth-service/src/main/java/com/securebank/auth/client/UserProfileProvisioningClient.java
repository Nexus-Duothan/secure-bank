package com.securebank.auth.client;

import com.securebank.auth.entity.UserCredential;

/**
 * Asks user-service to create the profile row for a customer we have just registered, so their
 * first sign-in reads real details instead of finding nothing.
 */
public interface UserProfileProvisioningClient {
  /**
   * Idempotent: calling it again for a customer who already has a profile changes nothing.
   *
   * @return true if user-service accepted the call
   */
  boolean provision(UserCredential user);
}
