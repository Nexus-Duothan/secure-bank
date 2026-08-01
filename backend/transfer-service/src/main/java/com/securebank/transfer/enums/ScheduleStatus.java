package com.securebank.transfer.enums;

public enum ScheduleStatus {
  ACTIVE,
  PAUSED,
  CANCELLED,
  /** Terminal: a one-time schedule executed successfully, or a recurring one reached its end date. */
  COMPLETED,
  /** Terminal: a one-time schedule's only run failed. Recurring schedules never land here - a
   *  failed occurrence just advances to the next one and stays ACTIVE. */
  FAILED,
}
