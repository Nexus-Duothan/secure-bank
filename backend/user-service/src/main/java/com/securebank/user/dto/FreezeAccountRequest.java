package com.securebank.user.dto;

import jakarta.validation.constraints.Size;

public record FreezeAccountRequest(@Size(max = 180) String reason) {}
