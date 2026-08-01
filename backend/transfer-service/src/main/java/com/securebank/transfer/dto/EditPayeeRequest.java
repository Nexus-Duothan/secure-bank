package com.securebank.transfer.dto;

import jakarta.validation.constraints.NotBlank;

public record EditPayeeRequest(@NotBlank String nickname) {}
