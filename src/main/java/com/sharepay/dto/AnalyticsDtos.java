package com.sharepay.dto;

import com.sharepay.dto.MemberDtos.MemberSummaryResponse;
import com.sharepay.dto.TransactionDtos.TransactionResponse;

import java.math.BigDecimal;
import java.util.List;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record CategoryBreakdown(String category, BigDecimal amount, BigDecimal percentage) {
    }

    public record MemberAmount(Long memberId, String displayName, BigDecimal amount) {
    }

    public record EventTotals(
            BigDecimal totalExpense,
            BigDecimal totalPaid,
            BigDecimal totalSponsored,
            BigDecimal netShared,
            long participantCount) {
    }

    public record DashboardResponse(
            EventTotals totals,
            List<CategoryBreakdown> expenseByCategory,
            List<MemberAmount> expenseByMember,
            List<MemberSummaryResponse> memberBalances,
            List<TransactionResponse> recentTransactions) {
    }

    public record SummaryResponse(
            EventTotals totals,
            List<CategoryBreakdown> expenseByCategory,
            List<MemberSummaryResponse> memberBalances) {
    }
}
