package com.sharepay.settlement;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SettlementOptimizerTest {

    private final SettlementOptimizer optimizer = new SettlementOptimizer();

    /** Applies the transfers to a copy of the balances and returns the residuals. */
    private Map<Long, BigDecimal> applyTransfers(Map<Long, BigDecimal> balances, java.util.List<Transfer> transfers) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>(balances);
        for (Transfer t : transfers) {
            // a debtor (negative) pays a creditor (positive): debtor balance rises, creditor falls
            result.merge(t.fromMemberId(), t.amount(), BigDecimal::add);
            result.merge(t.toMemberId(), t.amount().negate(), BigDecimal::add);
        }
        return result;
    }

    @Test
    void clearsAllBalancesToZero() {
        Map<Long, BigDecimal> balances = new LinkedHashMap<>();
        balances.put(1L, new BigDecimal("2750000"));   // Alice owed
        balances.put(2L, new BigDecimal("-1100000"));  // Bob owes
        balances.put(3L, new BigDecimal("-900000"));   // Charlie owes
        balances.put(4L, new BigDecimal("-750000"));   // David owes

        var transfers = optimizer.optimize(balances, "VND");

        Map<Long, BigDecimal> residual = applyTransfers(balances, transfers);
        residual.values().forEach(v -> assertThat(v.signum()).isZero());
    }

    @Test
    void minimizesTransferCountForCleanPairs() {
        // A->B and D->C should be enough (2 transfers, not 4).
        Map<Long, BigDecimal> balances = new LinkedHashMap<>();
        balances.put(1L, new BigDecimal("-50")); // A owes 50
        balances.put(2L, new BigDecimal("50"));  // B owed 50
        balances.put(3L, new BigDecimal("50"));  // C owed 50
        balances.put(4L, new BigDecimal("-50")); // D owes 50

        var transfers = optimizer.optimize(balances, "VND");

        assertThat(transfers).hasSize(2);
        Map<Long, BigDecimal> residual = applyTransfers(balances, transfers);
        residual.values().forEach(v -> assertThat(v.signum()).isZero());
    }

    @Test
    void producesAtMostNMinusOneTransfers() {
        Map<Long, BigDecimal> balances = new LinkedHashMap<>();
        balances.put(1L, new BigDecimal("100.00"));
        balances.put(2L, new BigDecimal("-30.00"));
        balances.put(3L, new BigDecimal("-30.00"));
        balances.put(4L, new BigDecimal("-40.00"));

        var transfers = optimizer.optimize(balances, "USD");

        assertThat(transfers.size()).isLessThanOrEqualTo(balances.size() - 1);
        assertThat(applyTransfers(balances, transfers).values())
                .allSatisfy(v -> assertThat(v.signum()).isZero());
    }

    @Test
    void returnsNothingWhenAllSettled() {
        Map<Long, BigDecimal> balances = new LinkedHashMap<>();
        balances.put(1L, BigDecimal.ZERO);
        balances.put(2L, BigDecimal.ZERO);
        assertThat(optimizer.optimize(balances, "VND")).isEmpty();
    }
}
