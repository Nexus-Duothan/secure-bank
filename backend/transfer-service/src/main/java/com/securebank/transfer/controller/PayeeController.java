package com.securebank.transfer.controller;

import com.securebank.transfer.dto.AddPayeeRequest;
import com.securebank.transfer.dto.ConfirmPayeeRequest;
import com.securebank.transfer.dto.EditPayeeRequest;
import com.securebank.transfer.dto.PayeeChallengeResponse;
import com.securebank.transfer.dto.PayeeResponse;
import com.securebank.transfer.security.CallerIdentity;
import com.securebank.transfer.service.PayeeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Saved-payee management (FR-16). Every route acts on the {@link CallerIdentity} resolved from
 * the caller's access token, never on a request-supplied user id.
 */
@RestController
@RequestMapping("/api/v1/transfers/payees")
@RequiredArgsConstructor
public class PayeeController {

  private final PayeeService payeeService;

  @GetMapping
  public ResponseEntity<List<PayeeResponse>> listPayees(CallerIdentity caller) {
    return ResponseEntity.ok(payeeService.listPayees(caller));
  }

  @PostMapping
  public ResponseEntity<PayeeChallengeResponse> requestAddPayee(
    CallerIdentity caller,
    @Valid @RequestBody AddPayeeRequest request
  ) {
    return ResponseEntity.ok(payeeService.requestAddPayee(caller, request));
  }

  @PostMapping("/{changeRequestId}/confirm")
  public ResponseEntity<PayeeResponse> confirmAddPayee(
    CallerIdentity caller,
    @PathVariable UUID changeRequestId,
    @Valid @RequestBody ConfirmPayeeRequest request
  ) {
    return ResponseEntity.ok(payeeService.confirmAddPayee(caller, changeRequestId, request));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<PayeeResponse> editPayee(
    CallerIdentity caller,
    @PathVariable UUID id,
    @Valid @RequestBody EditPayeeRequest request
  ) {
    return ResponseEntity.ok(payeeService.editPayee(caller, id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> removePayee(CallerIdentity caller, @PathVariable UUID id) {
    payeeService.removePayee(caller, id);
    return ResponseEntity.noContent().build();
  }
}
