package com.securebank.user.dto;

import com.securebank.user.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

/** A status decided elsewhere (auth-service) being mirrored onto the profile. */
public record ProfileStatusSyncRequest(@NotNull UserStatus status) {}
