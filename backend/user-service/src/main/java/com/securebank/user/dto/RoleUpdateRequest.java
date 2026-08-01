package com.securebank.user.dto;

import com.securebank.user.enums.Role;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(@NotNull Role role) {}
