package com.securebank.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceLinkRequest(
  @NotBlank @Size(max = 100) String deviceName,
  @Size(max = 60) String deviceType,
  @Size(max = 80) String browser,
  @Size(max = 100) String location
) {}
