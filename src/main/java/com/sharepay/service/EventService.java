package com.sharepay.service;

import com.sharepay.domain.Category;
import com.sharepay.domain.Event;
import com.sharepay.domain.EventMember;
import com.sharepay.domain.Transaction;
import com.sharepay.domain.Workspace;
import com.sharepay.domain.enums.EventStatus;
import com.sharepay.dto.EventDtos.DuplicateEventRequest;
import com.sharepay.dto.EventDtos.EventRequest;
import com.sharepay.dto.EventDtos.EventResponse;
import com.sharepay.dto.EventDtos.UpdateStatusRequest;
import com.sharepay.exception.NotFoundException;
import com.sharepay.repository.CategoryRepository;
import com.sharepay.repository.EventMemberRepository;
import com.sharepay.repository.EventRepository;
import com.sharepay.repository.LedgerEntryRepository;
import com.sharepay.repository.ReceiptRepository;
import com.sharepay.repository.TransactionRepository;
import com.sharepay.repository.WorkspaceRepository;
import com.sharepay.service.PermissionService.Action;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final WorkspaceRepository workspaceRepository;
    private final EventMemberRepository eventMemberRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ReceiptRepository receiptRepository;
    private final CategoryRepository categoryRepository;
    private final PermissionService permissionService;

    public EventService(EventRepository eventRepository,
                        WorkspaceRepository workspaceRepository,
                        EventMemberRepository eventMemberRepository,
                        TransactionRepository transactionRepository,
                        LedgerEntryRepository ledgerEntryRepository,
                        ReceiptRepository receiptRepository,
                        CategoryRepository categoryRepository,
                        PermissionService permissionService) {
        this.eventRepository = eventRepository;
        this.workspaceRepository = workspaceRepository;
        this.eventMemberRepository = eventMemberRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.receiptRepository = receiptRepository;
        this.categoryRepository = categoryRepository;
        this.permissionService = permissionService;
    }

    @Transactional
    public EventResponse create(Long userId, Long workspaceId, EventRequest request) {
        permissionService.require(workspaceId, userId, Action.MANAGE_EVENT);
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> NotFoundException.of("Workspace", workspaceId));
        Event event = new Event();
        event.setWorkspace(workspace);
        apply(event, request);
        event.setStatus(EventStatus.ACTIVE);
        return toResponse(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<EventResponse> list(Long userId, Long workspaceId, EventStatus status) {
        permissionService.require(workspaceId, userId, Action.VIEW);
        List<Event> events = (status == null)
                ? eventRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                : eventRepository.findByWorkspaceIdAndStatusOrderByCreatedAtDesc(workspaceId, status);
        return events.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EventResponse get(Long userId, Long eventId) {
        Event event = requireEvent(eventId);
        permissionService.require(event.getWorkspace().getId(), userId, Action.VIEW);
        return toResponse(event);
    }

    /** Loads an event after verifying the user may view it. Shared by other services. */
    @Transactional(readOnly = true)
    public Event getViewableEvent(Long userId, Long eventId) {
        Event event = requireEvent(eventId);
        permissionService.require(event.getWorkspace().getId(), userId, Action.VIEW);
        return event;
    }

    @Transactional
    public EventResponse update(Long userId, Long eventId, EventRequest request) {
        Event event = requireEvent(eventId);
        permissionService.require(event.getWorkspace().getId(), userId, Action.MANAGE_EVENT);
        apply(event, request);
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse updateStatus(Long userId, Long eventId, UpdateStatusRequest request) {
        Event event = requireEvent(eventId);
        permissionService.require(event.getWorkspace().getId(), userId, Action.MANAGE_EVENT);
        event.setStatus(request.status());
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public void delete(Long userId, Long eventId) {
        Event event = requireEvent(eventId);
        // Deleting events is an owner-level action per the permission matrix.
        permissionService.require(event.getWorkspace().getId(), userId, Action.MANAGE_WORKSPACE);

        List<Transaction> transactions = transactionRepository.findByEventIdOrderByDateDescIdDesc(eventId);
        for (Transaction tx : transactions) {
            receiptRepository.deleteAll(receiptRepository.findByTransactionId(tx.getId()));
        }
        ledgerEntryRepository.deleteAll(ledgerEntryRepository.findByEventId(eventId));
        transactionRepository.deleteAll(transactions);
        categoryRepository.deleteAll(categoryRepository.findByEventId(eventId));
        eventMemberRepository.deleteAll(eventMemberRepository.findByEventIdOrderByIdAsc(eventId));
        eventRepository.delete(event);
    }

    @Transactional
    public EventResponse duplicate(Long userId, Long eventId, DuplicateEventRequest request) {
        Event source = requireEvent(eventId);
        permissionService.require(source.getWorkspace().getId(), userId, Action.MANAGE_EVENT);

        Event copy = new Event();
        copy.setWorkspace(source.getWorkspace());
        copy.setName(Optional.ofNullable(request.name()).filter(n -> !n.isBlank()).map(String::trim)
                .orElse(source.getName() + " (Copy)"));
        copy.setDescription(source.getDescription());
        copy.setType(source.getType());
        copy.setCurrency(source.getCurrency());
        copy.setStartDate(source.getStartDate());
        copy.setEndDate(source.getEndDate());
        copy.setStatus(EventStatus.ACTIVE);
        copy = eventRepository.save(copy);

        if (request.copyMembers()) {
            for (EventMember source0 : eventMemberRepository.findByEventIdOrderByIdAsc(eventId)) {
                EventMember member = new EventMember();
                member.setEvent(copy);
                member.setDisplayName(source0.getDisplayName());
                member.setEmail(source0.getEmail());
                member.setPhone(source0.getPhone());
                member.setUser(source0.getUser());
                eventMemberRepository.save(member);
            }
        }
        // Event-specific categories are cloned; transactions and ledger are intentionally not.
        for (Category category : categoryRepository.findByEventId(eventId)) {
            categoryRepository.save(new Category(copy, category.getName()));
        }
        return toResponse(copy);
    }

    private void apply(Event event, EventRequest request) {
        event.setName(request.name().trim());
        event.setDescription(request.description());
        event.setType(request.type());
        event.setCurrency(request.currency().trim().toUpperCase());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
    }

    private EventResponse toResponse(Event e) {
        return new EventResponse(
                e.getId(),
                e.getWorkspace().getId(),
                e.getName(),
                e.getDescription(),
                e.getType(),
                e.getCurrency(),
                e.getStartDate(),
                e.getEndDate(),
                e.getStatus(),
                eventMemberRepository.countByEventId(e.getId()),
                e.getSettledAt(),
                e.getCreatedAt());
    }

    private Event requireEvent(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> NotFoundException.of("Event", id));
    }
}
