package com.sharepay.web;

import com.sharepay.dto.SettlementDtos.SettlementResponse;
import com.sharepay.security.AppUserPrincipal;
import com.sharepay.service.SettlementService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/{eventId}/settlement")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping
    public SettlementResponse getSettlement(@AuthenticationPrincipal AppUserPrincipal principal,
                                            @PathVariable Long eventId) {
        return settlementService.getSettlement(principal.getId(), eventId);
    }

    @PostMapping("/settle")
    public SettlementResponse markSettled(@AuthenticationPrincipal AppUserPrincipal principal,
                                          @PathVariable Long eventId) {
        return settlementService.markSettled(principal.getId(), eventId);
    }
}
