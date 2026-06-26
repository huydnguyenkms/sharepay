package com.sharepay.web;

import com.sharepay.dto.TransactionDtos.CreateAdjustmentRequest;
import com.sharepay.dto.TransactionDtos.CreateExpenseRequest;
import com.sharepay.dto.TransactionDtos.CreateRefundRequest;
import com.sharepay.dto.TransactionDtos.ReceiptRef;
import com.sharepay.dto.TransactionDtos.TransactionResponse;
import com.sharepay.security.AppUserPrincipal;
import com.sharepay.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<TransactionResponse> list(@AuthenticationPrincipal AppUserPrincipal principal,
                                          @PathVariable Long eventId) {
        return transactionService.list(principal.getId(), eventId);
    }

    @GetMapping("/{txId}")
    public TransactionResponse get(@AuthenticationPrincipal AppUserPrincipal principal,
                                   @PathVariable Long eventId,
                                   @PathVariable Long txId) {
        return transactionService.get(principal.getId(), eventId, txId);
    }

    @PostMapping("/expenses")
    public ResponseEntity<TransactionResponse> createExpense(@AuthenticationPrincipal AppUserPrincipal principal,
                                                             @PathVariable Long eventId,
                                                             @Valid @RequestBody CreateExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createExpense(principal.getId(), eventId, request));
    }

    @PutMapping("/expenses/{txId}")
    public TransactionResponse updateExpense(@AuthenticationPrincipal AppUserPrincipal principal,
                                             @PathVariable Long eventId,
                                             @PathVariable Long txId,
                                             @Valid @RequestBody CreateExpenseRequest request) {
        return transactionService.updateExpense(principal.getId(), eventId, txId, request);
    }

    @PostMapping("/refunds")
    public ResponseEntity<TransactionResponse> createRefund(@AuthenticationPrincipal AppUserPrincipal principal,
                                                            @PathVariable Long eventId,
                                                            @Valid @RequestBody CreateRefundRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createRefund(principal.getId(), eventId, request));
    }

    @PostMapping("/adjustments")
    public ResponseEntity<TransactionResponse> createAdjustment(@AuthenticationPrincipal AppUserPrincipal principal,
                                                                @PathVariable Long eventId,
                                                                @Valid @RequestBody CreateAdjustmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createAdjustment(principal.getId(), eventId, request));
    }

    @DeleteMapping("/{txId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserPrincipal principal,
                       @PathVariable Long eventId,
                       @PathVariable Long txId) {
        transactionService.delete(principal.getId(), eventId, txId);
    }

    @PostMapping("/{txId}/receipts")
    public ResponseEntity<ReceiptRef> uploadReceipt(@AuthenticationPrincipal AppUserPrincipal principal,
                                                    @PathVariable Long eventId,
                                                    @PathVariable Long txId,
                                                    @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.uploadReceipt(principal.getId(), eventId, txId, file));
    }
}
