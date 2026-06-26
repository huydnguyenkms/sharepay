package com.sharepay.service;

import com.sharepay.common.MoneyUtil;
import com.sharepay.domain.Event;
import com.sharepay.domain.EventMember;
import com.sharepay.domain.Transaction;
import com.sharepay.domain.enums.TransactionType;
import com.sharepay.dto.AnalyticsDtos.CategoryBreakdown;
import com.sharepay.dto.AnalyticsDtos.DashboardResponse;
import com.sharepay.dto.AnalyticsDtos.EventTotals;
import com.sharepay.dto.AnalyticsDtos.MemberAmount;
import com.sharepay.dto.AnalyticsDtos.SummaryResponse;
import com.sharepay.dto.MemberDtos.MemberSummaryResponse;
import com.sharepay.dto.TransactionDtos.TransactionResponse;
import com.sharepay.ledger.BalanceBreakdown;
import com.sharepay.ledger.BalanceService;
import com.sharepay.repository.EventMemberRepository;
import com.sharepay.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private static final int RECENT_LIMIT = 5;

    private final EventService eventService;
    private final BalanceService balanceService;
    private final TransactionRepository transactionRepository;
    private final EventMemberRepository eventMemberRepository;
    private final TransactionService transactionService;

    public AnalyticsService(EventService eventService,
                            BalanceService balanceService,
                            TransactionRepository transactionRepository,
                            EventMemberRepository eventMemberRepository,
                            TransactionService transactionService) {
        this.eventService = eventService;
        this.balanceService = balanceService;
        this.transactionRepository = transactionRepository;
        this.eventMemberRepository = eventMemberRepository;
        this.transactionService = transactionService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(Long userId, Long eventId) {
        Event event = eventService.getViewableEvent(userId, eventId);
        Map<Long, BalanceBreakdown> balances = balanceService.computeBalances(event);
        Map<Long, String> names = memberNames(eventId);
        List<Transaction> transactions = transactionRepository.findByEventIdOrderByDateDescIdDesc(eventId);

        List<TransactionResponse> recent = transactionService.list(userId, eventId).stream()
                .limit(RECENT_LIMIT)
                .toList();

        return new DashboardResponse(
                totals(event, balances),
                expenseByCategory(event, transactions),
                expenseByMember(balances, names),
                memberSummaries(balances, names),
                recent);
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary(Long userId, Long eventId) {
        Event event = eventService.getViewableEvent(userId, eventId);
        Map<Long, BalanceBreakdown> balances = balanceService.computeBalances(event);
        Map<Long, String> names = memberNames(eventId);
        List<Transaction> transactions = transactionRepository.findByEventIdOrderByDateDescIdDesc(eventId);
        return new SummaryResponse(
                totals(event, balances),
                expenseByCategory(event, transactions),
                memberSummaries(balances, names));
    }

    @Transactional(readOnly = true)
    public List<MemberSummaryResponse> memberSummaries(Long userId, Long eventId) {
        Event event = eventService.getViewableEvent(userId, eventId);
        return memberSummaries(balanceService.computeBalances(event), memberNames(eventId));
    }

    // --- internals ---

    private EventTotals totals(Event event, Map<Long, BalanceBreakdown> balances) {
        String currency = event.getCurrency();
        BigDecimal totalPaid = MoneyUtil.zero(currency);
        BigDecimal totalShare = MoneyUtil.zero(currency);
        BigDecimal totalSponsored = MoneyUtil.zero(currency);
        for (BalanceBreakdown b : balances.values()) {
            totalPaid = totalPaid.add(b.paid());
            totalShare = totalShare.add(b.share());
            totalSponsored = totalSponsored.add(b.sponsored());
        }
        // Net shared (what participants ultimately owe) = total expense minus sponsorship.
        BigDecimal netShared = totalShare;
        BigDecimal totalExpense = totalShare.add(totalSponsored);
        return new EventTotals(
                MoneyUtil.round(totalExpense, currency),
                MoneyUtil.round(totalPaid, currency),
                MoneyUtil.round(totalSponsored, currency),
                MoneyUtil.round(netShared, currency),
                balances.size());
    }

    private List<CategoryBreakdown> expenseByCategory(Event event, List<Transaction> transactions) {
        String currency = event.getCurrency();
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        BigDecimal total = MoneyUtil.zero(currency);
        for (Transaction tx : transactions) {
            if (tx.getType() != TransactionType.EXPENSE) {
                continue;
            }
            String name = tx.getCategory() != null ? tx.getCategory().getName() : "Uncategorized";
            byCategory.merge(name, tx.getAmount(), BigDecimal::add);
            total = total.add(tx.getAmount());
        }
        final BigDecimal grandTotal = total;
        List<CategoryBreakdown> result = new ArrayList<>();
        byCategory.forEach((name, amount) -> {
            BigDecimal pct = grandTotal.signum() == 0 ? BigDecimal.ZERO
                    : amount.multiply(BigDecimal.valueOf(100)).divide(grandTotal, 1, RoundingMode.HALF_UP);
            result.add(new CategoryBreakdown(name, MoneyUtil.round(amount, currency), pct));
        });
        result.sort((a, b) -> b.amount().compareTo(a.amount()));
        return result;
    }

    private List<MemberAmount> expenseByMember(Map<Long, BalanceBreakdown> balances, Map<Long, String> names) {
        List<MemberAmount> result = new ArrayList<>();
        balances.forEach((id, b) -> result.add(new MemberAmount(id, names.get(id), b.share())));
        result.sort((a, b) -> b.amount().compareTo(a.amount()));
        return result;
    }

    private List<MemberSummaryResponse> memberSummaries(Map<Long, BalanceBreakdown> balances, Map<Long, String> names) {
        List<MemberSummaryResponse> result = new ArrayList<>();
        balances.forEach((id, b) -> result.add(new MemberSummaryResponse(
                id, names.get(id), b.paid(), b.share(), b.sponsored(), b.balance())));
        return result;
    }

    private Map<Long, String> memberNames(Long eventId) {
        Map<Long, String> names = new LinkedHashMap<>();
        for (EventMember m : eventMemberRepository.findByEventIdOrderByIdAsc(eventId)) {
            names.put(m.getId(), m.getDisplayName());
        }
        return names;
    }
}
