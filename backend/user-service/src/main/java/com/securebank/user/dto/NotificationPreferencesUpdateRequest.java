package com.securebank.user.dto;

public record NotificationPreferencesUpdateRequest(boolean email, boolean sms, boolean push) {}
