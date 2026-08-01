package com.securebank.user.dto;

import com.securebank.user.enums.Role;
import java.util.UUID;

/** Staged payload for an OTP-confirmed role change performed by an administrator. */
public record AdminRoleChangePayload(UUID targetUserId, Role role) {}
