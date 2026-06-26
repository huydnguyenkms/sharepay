package com.sharepay.ledger;

import com.sharepay.common.MoneyUtil;
import com.sharepay.domain.Event;
import com.sharepay.domain.EventMember;
import com.sharepay.domain.LedgerEntry;
import com.sharepay.repository.EventMemberRepository;
import com.sharepay.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Derives member balances purely from ledger entries. No balance is ever stored.
 */
@Service
public class BalanceService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final EventMemberRepository eventMemberRepository;

    public BalanceService(LedgerEntryRepository ledgerEntryRepository,
                          EventMemberRepository eventMemberRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.eventMemberRepository = eventMemberRepository;
    }

    /** Ordered map (by member id) of every event member's balance breakdown, zeros included. */
    @Transactional(readOnly = true)
    public Map<Long, BalanceBreakdown> computeBalances(Event event) {
        String currency = event.getCurrency();

        // Accumulators, seeded with every member so non-participating members show zeros.
        Map<Long, BigDecimal> paid = new LinkedHashMap<>();
        Map<Long, BigDecimal> share = new LinkedHashMap<>();
        Map<Long, BigDecimal> sponsored = new LinkedHashMap<>();
        Map<Long, BigDecimal> refunds = new LinkedHashMap<>();
        Map<Long, BigDecimal> adjustments = new LinkedHashMap<>();

        List<EventMember> members = eventMemberRepository.findByEventIdOrderByIdAsc(event.getId());
        for (EventMember member : members) {
            BigDecimal zero = MoneyUtil.zero(currency);
            paid.put(member.getId(), zero);
            share.put(member.getId(), zero);
            sponsored.put(member.getId(), zero);
            refunds.put(member.getId(), zero);
            adjustments.put(member.getId(), zero);
        }

        for (LedgerEntry entry : ledgerEntryRepository.findByEventId(event.getId())) {
            Long memberId = entry.getMember().getId();
            BigDecimal amount = entry.getAmount();
            switch (entry.getType()) {
                case PAYMENT_CREDIT -> paid.merge(memberId, amount, BigDecimal::add);
                case SHARE_DEBIT -> share.merge(memberId, amount, BigDecimal::add);
                case SPONSOR_DEBIT -> sponsored.merge(memberId, amount, BigDecimal::add);
                case REFUND_CREDIT -> refunds.merge(memberId, amount, BigDecimal::add);
                case REFUND_DEBIT -> refunds.merge(memberId, amount.negate(), BigDecimal::add);
                case ADJUSTMENT_CREDIT -> adjustments.merge(memberId, amount, BigDecimal::add);
                case ADJUSTMENT_DEBIT -> adjustments.merge(memberId, amount.negate(), BigDecimal::add);
            }
        }

        Map<Long, BalanceBreakdown> result = new LinkedHashMap<>();
        for (EventMember member : members) {
            Long id = member.getId();
            BigDecimal p = paid.get(id);
            BigDecimal s = share.get(id);
            BigDecimal sp = sponsored.get(id);
            BigDecimal r = refunds.get(id);
            BigDecimal adj = adjustments.get(id);
            BigDecimal balance = p.subtract(s).subtract(sp).add(r).add(adj);
            result.put(id, new BalanceBreakdown(
                    id,
                    MoneyUtil.round(p, currency),
                    MoneyUtil.round(s, currency),
                    MoneyUtil.round(sp, currency),
                    MoneyUtil.round(r, currency),
                    MoneyUtil.round(adj, currency),
                    MoneyUtil.round(balance, currency)));
        }
        return result;
    }
}
