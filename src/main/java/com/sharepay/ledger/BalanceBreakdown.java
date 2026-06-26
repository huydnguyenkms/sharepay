package com.sharepay.ledger;

import java.math.BigDecimal;

/**
 * A member's derived position within an event, matching the spec balance formula:
 * {@code balance = paid - share - sponsored + refunds + adjustments}, where refunds and
 * adjustments are the net (credit - debit) of those entry types.
 */
public record BalanceBreakdown(
        Long memberId,
        BigDecimal paid,
        BigDecimal share,
        BigDecimal sponsored,
        BigDecimal refunds,
        BigDecimal adjustments,
        BigDecimal balance
) {
}
