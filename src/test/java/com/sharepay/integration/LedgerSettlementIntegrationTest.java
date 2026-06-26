package com.sharepay.integration;

import com.sharepay.domain.Event;
import com.sharepay.domain.enums.EventType;
import com.sharepay.domain.enums.SplitMethod;
import com.sharepay.dto.EventDtos.EventRequest;
import com.sharepay.dto.MemberDtos.EventMemberRequest;
import com.sharepay.dto.TransactionDtos.CreateAdjustmentRequest;
import com.sharepay.dto.TransactionDtos.CreateExpenseRequest;
import com.sharepay.dto.TransactionDtos.CreateRefundRequest;
import com.sharepay.dto.TransactionDtos.ParticipantInput;
import com.sharepay.dto.TransactionDtos.PayerInput;
import com.sharepay.dto.TransactionDtos.SponsorInput;
import com.sharepay.dto.SettlementDtos.SettlementResponse;
import com.sharepay.dto.SettlementDtos.TransferResponse;
import com.sharepay.dto.WorkspaceDtos.WorkspaceRequest;
import com.sharepay.ledger.BalanceBreakdown;
import com.sharepay.ledger.BalanceService;
import com.sharepay.repository.EventRepository;
import com.sharepay.service.AuthService;
import com.sharepay.service.EventMemberService;
import com.sharepay.service.EventService;
import com.sharepay.service.SettlementService;
import com.sharepay.service.TransactionService;
import com.sharepay.service.WorkspaceService;
import com.sharepay.dto.AuthDtos.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LedgerSettlementIntegrationTest {

    @Autowired AuthService authService;
    @Autowired WorkspaceService workspaceService;
    @Autowired EventService eventService;
    @Autowired EventMemberService eventMemberService;
    @Autowired TransactionService transactionService;
    @Autowired BalanceService balanceService;
    @Autowired SettlementService settlementService;
    @Autowired EventRepository eventRepository;

    private record Setup(Long userId, Long eventId, List<Long> memberIds) {
    }

    private Setup bootstrap(String email) {
        Long userId = authService.register(new RegisterRequest(email, "password123", "Tester")).user().id();
        Long wsId = workspaceService.create(userId, new WorkspaceRequest("WS", null)).id();
        Long eventId = eventService.create(userId, wsId, new EventRequest(
                "Trip", null, EventType.TRAVEL, "VND", LocalDate.now(), LocalDate.now())).id();
        List<Long> members = List.of("A", "B", "C", "D").stream()
                .map(n -> eventMemberService.add(userId, eventId,
                        new EventMemberRequest(n, null, null, null)).id())
                .toList();
        return new Setup(userId, eventId, members);
    }

    private BigDecimal sumOfBalances(Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow();
        Map<Long, BalanceBreakdown> balances = balanceService.computeBalances(event);
        return balances.values().stream().map(BalanceBreakdown::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void singleExpenseProducesExpectedBalances() {
        Setup s = bootstrap("ledger1@example.com");
        Long payer = s.memberIds().get(0);

        transactionService.createExpense(s.userId(), s.eventId(), new CreateExpenseRequest(
                "Hotel", null, new BigDecimal("1200"), LocalDate.now(), null, SplitMethod.EQUAL,
                List.of(new PayerInput(payer, new BigDecimal("1200"))),
                s.memberIds().stream().map(id -> new ParticipantInput(id, null)).toList(),
                List.of()));

        Event event = eventRepository.findById(s.eventId()).orElseThrow();
        Map<Long, BalanceBreakdown> balances = balanceService.computeBalances(event);

        // Payer paid 1200, owes 300 -> +900; each of the other three -> -300.
        assertThat(balances.get(payer).balance()).isEqualByComparingTo("900");
        s.memberIds().stream().skip(1).forEach(id ->
                assertThat(balances.get(id).balance()).isEqualByComparingTo("-300"));
        assertThat(sumOfBalances(s.eventId())).isEqualByComparingTo("0");
    }

    @Test
    void mixedFlowsStayZeroSumAndSettleCleanly() {
        Setup s = bootstrap("ledger2@example.com");
        var ids = s.memberIds();
        var allParticipants = ids.stream().map(id -> new ParticipantInput(id, null)).toList();

        transactionService.createExpense(s.userId(), s.eventId(), new CreateExpenseRequest(
                "Hotel", null, new BigDecimal("1200"), LocalDate.now(), null, SplitMethod.EQUAL,
                List.of(new PayerInput(ids.get(0), new BigDecimal("1200"))), allParticipants, List.of()));

        // Dinner 1000, paid by B, C sponsors 400 -> net 600 split among all four.
        transactionService.createExpense(s.userId(), s.eventId(), new CreateExpenseRequest(
                "Dinner", null, new BigDecimal("1000"), LocalDate.now(), null, SplitMethod.EQUAL,
                List.of(new PayerInput(ids.get(1), new BigDecimal("1000"))), allParticipants,
                List.of(new SponsorInput(ids.get(2), new BigDecimal("400")))));

        transactionService.createRefund(s.userId(), s.eventId(), new CreateRefundRequest(
                "Refund", null, new BigDecimal("200"), LocalDate.now(), null, ids.get(0), ids));

        transactionService.createAdjustment(s.userId(), s.eventId(), new CreateAdjustmentRequest(
                "Adjust", new BigDecimal("100"), LocalDate.now(), null, ids.get(3), ids.get(0)));

        assertThat(sumOfBalances(s.eventId())).isEqualByComparingTo("0");

        SettlementResponse settlement = settlementService.getSettlement(s.userId(), s.eventId());
        assertThat(settlement.transferCount()).isLessThanOrEqualTo(ids.size() - 1);

        // Settlement transfers must net every balance back to zero.
        Event event = eventRepository.findById(s.eventId()).orElseThrow();
        Map<Long, BalanceBreakdown> balances = balanceService.computeBalances(event);
        Map<Long, BigDecimal> residual = new java.util.LinkedHashMap<>();
        balances.forEach((id, b) -> residual.put(id, b.balance()));
        for (TransferResponse t : settlement.transfers()) {
            residual.merge(t.fromMemberId(), t.amount(), BigDecimal::add);
            residual.merge(t.toMemberId(), t.amount().negate(), BigDecimal::add);
        }
        residual.values().forEach(v -> assertThat(v.signum()).isZero());
    }
}
