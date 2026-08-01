package com.securebank.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
  @Size(max = 120) String fullName,
  @Email @Size(max = 120) String email,
  @Size(max = 30) String phoneNumber,
  @Size(max = 180) String addressLine,
  @Size(max = 80) String city,
  @Size(max = 80) String country,
  @Size(max = 40) String language
) {}
