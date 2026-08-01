package com.securebank.notification;

public record NotificationResponse(
  String id,
  String type,
  String title,
  String description,
  String categoryLabel,
  String timestamp,
  String groupLabel,
  boolean read
) {}
