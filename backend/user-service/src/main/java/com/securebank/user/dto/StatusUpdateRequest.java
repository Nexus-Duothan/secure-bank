package com.securebank.user.dto;

import com.securebank.user.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull UserStatus status) {}
