package com.sharepay.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class SettlementDtos {

    private SettlementDtos() {
    }

    public record TransferResponse(
            Long fromMemberId,
            String fromName,
            Long toMemberId,
            String toName,
            BigDecimal amount) {
    }

    public record SettlementResponse(
            List<TransferResponse> transfers,
            int transferCount,
            BigDecimal totalAmount,
            boolean settled,
            LocalDateTime settledAt) {
    }
}
