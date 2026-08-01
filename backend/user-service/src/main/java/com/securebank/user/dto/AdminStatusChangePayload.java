package com.securebank.user.dto;

import com.securebank.user.enums.UserStatus;
import java.util.UUID;

/** Staged payload for an OTP-confirmed account status change performed by bank staff. */
public record AdminStatusChangePayload(UUID targetUserId, UserStatus status) {}
