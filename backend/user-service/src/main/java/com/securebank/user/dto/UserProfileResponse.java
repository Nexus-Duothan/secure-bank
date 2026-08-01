package com.securebank.user.dto;

import com.securebank.user.enums.Role;
import com.securebank.user.enums.UserStatus;
import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
  UUID id,
  String fullName,
  String email,
  String phoneNumber,
  String addressLine,
  String city,
  String country,
  String language,
  Role role,
  UserStatus status,
  boolean idVerified,
  NotificationPreferencesResponse notificationPreferences,
  List<UserDeviceResponse> linkedDevices
) {}
