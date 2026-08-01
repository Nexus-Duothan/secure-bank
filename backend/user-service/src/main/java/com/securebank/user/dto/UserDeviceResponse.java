package com.securebank.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserDeviceResponse(
  UUID id,
  String deviceName,
  String deviceType,
  String browser,
  String location,
  boolean trusted,
  Instant lastVerifiedAt
) {}
