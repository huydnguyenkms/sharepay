package com.sharepay.service;

import com.sharepay.common.MoneyUtil;
import com.sharepay.domain.Event;
import com.sharepay.domain.EventMember;
import com.sharepay.domain.enums.EventStatus;
import com.sharepay.dto.SettlementDtos.SettlementResponse;
import com.sharepay.dto.SettlementDtos.TransferResponse;
import com.sharepay.ledger.BalanceBreakdown;
import com.sharepay.ledger.BalanceService;
import com.sharepay.repository.EventMemberRepository;
import com.sharepay.repository.EventRepository;
import com.sharepay.settlement.SettlementOptimizer;
import com.sharepay.settlement.Transfer;
import com.sharepay.service.PermissionService.Action;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SettlementService {

    private final EventService eventService;
    private final EventRepository eventRepository;
    private final BalanceService balanceService;
    private final SettlementOptimizer optimizer;
    private final EventMemberRepository eventMemberRepository;
    private final PermissionService permissionService;

    public SettlementService(EventService eventService,
                             EventRepository eventRepository,
                             BalanceService balanceService,
                             SettlementOptimizer optimizer,
                             EventMemberRepository eventMemberRepository,
                             PermissionService permissionService) {
        this.eventService = eventService;
        this.eventRepository = eventRepository;
        this.balanceService = balanceService;
        this.optimizer = optimizer;
        this.eventMemberRepository = eventMemberRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(Long userId, Long eventId) {
        Event event = eventService.getViewableEvent(userId, eventId);
        return buildSettlement(event);
    }

    @Transactional
    public SettlementResponse markSettled(Long userId, Long eventId) {
        Event event = eventService.getViewableEvent(userId, eventId);
        permissionService.require(event.getWorkspace().getId(), userId, Action.GENERATE_SETTLEMENT);
        event.setSettledAt(LocalDateTime.now());
        event.setStatus(EventStatus.COMPLETED);
        eventRepository.save(event);
        return buildSettlement(event);
    }

    private SettlementResponse buildSettlement(Event event) {
        Map<Long, BalanceBreakdown> breakdowns = balanceService.computeBalances(event);
        Map<Long, BigDecimal> balances = new LinkedHashMap<>();
        breakdowns.forEach((id, b) -> balances.put(id, b.balance()));

        Map<Long, String> names = new LinkedHashMap<>();
        for (EventMember m : eventMemberRepository.findByEventIdOrderByIdAsc(event.getId())) {
            names.put(m.getId(), m.getDisplayName());
        }

        List<Transfer> transfers = optimizer.optimize(balances, event.getCurrency());
        BigDecimal total = MoneyUtil.zero(event.getCurrency());
        List<TransferResponse> responses = new java.util.ArrayList<>();
        for (Transfer t : transfers) {
            responses.add(new TransferResponse(
                    t.fromMemberId(), names.get(t.fromMemberId()),
                    t.toMemberId(), names.get(t.toMemberId()),
                    t.amount()));
            total = total.add(t.amount());
        }
        return new SettlementResponse(
                responses,
                responses.size(),
                MoneyUtil.round(total, event.getCurrency()),
                event.getSettledAt() != null,
                event.getSettledAt());
    }
}
