package com.securebank.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmPayeeRequest(@NotBlank @Size(min = 6, max = 6) String otpCode) {}
