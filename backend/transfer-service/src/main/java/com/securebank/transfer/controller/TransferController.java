package com.securebank.transfer.controller;

import com.securebank.transfer.dto.ConfirmTransferRequest;
import com.securebank.transfer.dto.TransferQuoteRequest;
import com.securebank.transfer.dto.TransferResponse;
import com.securebank.transfer.security.CallerIdentity;
import com.securebank.transfer.service.TransferService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal Account-to-Account transfers (FR-14). Every route acts on the {@link CallerIdentity}
 * resolved from the caller's access token, never on a request-supplied user id.
 */
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

  private final TransferService transferService;

  /**
   * Stages a transfer for review (FR-17): validates the recipient, balance and limits, and
   * returns a summary the client should display before calling {@link #confirm}.
   */
  @PostMapping("/quote")
  public ResponseEntity<TransferResponse> quote(
    CallerIdentity caller,
    @Valid @RequestBody TransferQuoteRequest request,
    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
  ) {
    return ResponseEntity.ok(transferService.quote(caller, request, idempotencyKey));
  }

  /** Executes a previously quoted transfer. Safe to retry: re-confirming a completed transfer just returns it. */
  @PostMapping("/{id}/confirm")
  public ResponseEntity<TransferResponse> confirm(
    CallerIdentity caller,
    @PathVariable UUID id,
    @Valid @RequestBody ConfirmTransferRequest request
  ) {
    return ResponseEntity.ok(transferService.confirm(caller, id, request.totpCode()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<TransferResponse> getTransfer(
    CallerIdentity caller,
    @PathVariable UUID id
  ) {
    return ResponseEntity.ok(transferService.get(caller, id));
  }
}
