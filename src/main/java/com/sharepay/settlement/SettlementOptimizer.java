package com.sharepay.settlement;

import com.sharepay.common.MoneyUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Greedy minimum-cash-flow settlement. Given each member's net balance (positive = owed
 * money, negative = owes), it repeatedly matches the largest debtor against the largest
 * creditor and emits a transfer for the smaller of the two magnitudes. This yields at most
 * n-1 transfers and clears every balance — e.g. it turns four pairwise debts (A->B, A->C,
 * D->B, D->C) into the minimal A->B, D->C when the amounts line up.
 *
 * <p>Balances are expected to sum to zero (guaranteed by the double-entry ledger).
 */
@Component
public class SettlementOptimizer {

    private record Node(Long memberId, BigDecimal amount) {
    }

    public List<Transfer> optimize(Map<Long, BigDecimal> balances, String currency) {
        int scale = MoneyUtil.scaleFor(currency);
        BigDecimal epsilon = BigDecimal.ONE.movePointLeft(scale); // one minor unit

        // Max-heaps by magnitude, with a stable tie-break by memberId for determinism.
        Comparator<Node> byAmountDesc = Comparator
                .comparing(Node::amount, Comparator.reverseOrder())
                .thenComparing(Node::memberId);
        PriorityQueue<Node> debtors = new PriorityQueue<>(byAmountDesc);
        PriorityQueue<Node> creditors = new PriorityQueue<>(byAmountDesc);

        balances.forEach((memberId, balance) -> {
            BigDecimal rounded = MoneyUtil.round(balance, currency);
            if (rounded.signum() > 0) {
                creditors.add(new Node(memberId, rounded));
            } else if (rounded.signum() < 0) {
                debtors.add(new Node(memberId, rounded.negate()));
            }
        });

        List<Transfer> transfers = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Node debtor = debtors.poll();
            Node creditor = creditors.poll();

            BigDecimal amount = debtor.amount().min(creditor.amount());
            transfers.add(new Transfer(debtor.memberId(), creditor.memberId(), amount));

            BigDecimal debtorRemaining = debtor.amount().subtract(amount);
            BigDecimal creditorRemaining = creditor.amount().subtract(amount);

            if (debtorRemaining.compareTo(epsilon) >= 0) {
                debtors.add(new Node(debtor.memberId(), debtorRemaining));
            }
            if (creditorRemaining.compareTo(epsilon) >= 0) {
                creditors.add(new Node(creditor.memberId(), creditorRemaining));
            }
        }
        return transfers;
    }
}
