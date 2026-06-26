package com.sharepay.web;

import com.sharepay.domain.Receipt;
import com.sharepay.security.AppUserPrincipal;
import com.sharepay.service.TransactionService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final TransactionService transactionService;

    public ReceiptController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/{receiptId}")
    public ResponseEntity<ByteArrayResource> download(@AuthenticationPrincipal AppUserPrincipal principal,
                                                      @PathVariable Long receiptId) {
        Receipt receipt = transactionService.getReceipt(principal.getId(), receiptId);
        ByteArrayResource resource = new ByteArrayResource(receipt.getData());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(receipt.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(receipt.getFileName()).toString())
                .contentLength(receipt.getSizeBytes())
                .body(resource);
    }
}
