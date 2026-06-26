package com.sharepay.web;

import com.sharepay.security.AppUserPrincipal;
import com.sharepay.service.ExportService;
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
@RequestMapping("/api/events/{eventId}/export")
public class ExportController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/excel")
    public ResponseEntity<ByteArrayResource> excel(@AuthenticationPrincipal AppUserPrincipal principal,
                                                   @PathVariable Long eventId) {
        byte[] bytes = exportService.exportExcel(principal.getId(), eventId);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("event-" + eventId + ".xlsx").toString())
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }

    @GetMapping("/csv")
    public ResponseEntity<ByteArrayResource> csv(@AuthenticationPrincipal AppUserPrincipal principal,
                                                 @PathVariable Long eventId) {
        byte[] bytes = exportService.exportCsv(principal.getId(), eventId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("event-" + eventId + ".csv").toString())
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }
}
