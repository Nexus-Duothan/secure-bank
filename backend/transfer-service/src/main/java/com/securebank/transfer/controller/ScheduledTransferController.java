package com.securebank.transfer.controller;

import com.securebank.transfer.dto.CreateScheduledTransferRequest;
import com.securebank.transfer.dto.ScheduledTransferResponse;
import com.securebank.transfer.dto.UpdateScheduleStatusRequest;
import com.securebank.transfer.security.CallerIdentity;
import com.securebank.transfer.service.ScheduledTransferService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Scheduled and recurring payments (FR-19). Every route acts on the {@link CallerIdentity}
 * resolved from the caller's access token, never on a request-supplied user id.
 */
@RestController
@RequestMapping("/api/v1/transfers/scheduled")
@RequiredArgsConstructor
public class ScheduledTransferController {

  private final ScheduledTransferService scheduledTransferService;

  @PostMapping
  public ResponseEntity<ScheduledTransferResponse> create(
    CallerIdentity caller,
    @Valid @RequestBody CreateScheduledTransferRequest request
  ) {
    return ResponseEntity.ok(scheduledTransferService.create(caller, request));
  }

  @GetMapping
  public ResponseEntity<List<ScheduledTransferResponse>> list(CallerIdentity caller) {
    return ResponseEntity.ok(scheduledTransferService.list(caller));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<ScheduledTransferResponse> updateStatus(
    CallerIdentity caller,
    @PathVariable UUID id,
    @Valid @RequestBody UpdateScheduleStatusRequest request
  ) {
    return ResponseEntity.ok(scheduledTransferService.updateStatus(caller, id, request));
  }
}
