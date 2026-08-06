package com.securebank.user.dto;

import java.time.Instant;

/**
 * The real, just-measured state of one platform service.
 *
 * @param key stable identifier, so the UI does not key off the display name
 * @param name how the service is named in the admin portal
 * @param status UP when it answered healthy, DOWN when it answered unhealthy or could not be
 *     reached at all, UNKNOWN when no address is configured for it
 * @param detail why it is not UP; null when it is
 * @param responseTimeMs how long the probe took, so a service that is up but crawling is visible
 * @param checkedAt when this was measured - never a cached or assumed value
 */
public record ServiceHealthResponse(
  String key,
  String name,
  String status,
  String detail,
  Long responseTimeMs,
  Instant checkedAt
) {}
