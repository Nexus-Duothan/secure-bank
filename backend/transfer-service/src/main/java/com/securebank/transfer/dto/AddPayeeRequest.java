package com.securebank.transfer.dto;

import jakarta.validation.constraints.NotBlank;

public record AddPayeeRequest(@NotBlank String nickname, @NotBlank String accountReference) {}
