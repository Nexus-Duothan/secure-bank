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

  /**
   * Mirrors a status this service has decided (a KYC outcome) onto the customer's profile, so the
   * app stops showing them as still under review once an officer has ruled.
   *
   * @return true if user-service accepted the call
   */
  boolean syncStatus(java.util.UUID userId, com.securebank.auth.enums.UserStatus status);
}
