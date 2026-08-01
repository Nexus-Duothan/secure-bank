package com.securebank.accounts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmChangeRequest(@NotBlank @Size(min = 6, max = 6) String otpCode) {}
