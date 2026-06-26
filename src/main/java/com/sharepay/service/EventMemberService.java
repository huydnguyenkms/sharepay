package com.sharepay.service;

import com.sharepay.domain.Event;
import com.sharepay.domain.EventMember;
import com.sharepay.domain.User;
import com.sharepay.dto.MemberDtos.EventMemberRequest;
import com.sharepay.dto.MemberDtos.EventMemberResponse;
import com.sharepay.exception.BadRequestException;
import com.sharepay.exception.NotFoundException;
import com.sharepay.repository.EventMemberRepository;
import com.sharepay.repository.LedgerEntryRepository;
import com.sharepay.repository.UserRepository;
import com.sharepay.service.PermissionService.Action;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventMemberService {

    private final EventMemberRepository eventMemberRepository;
    private final UserRepository userRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final EventService eventService;
    private final PermissionService permissionService;

    public EventMemberService(EventMemberRepository eventMemberRepository,
                              UserRepository userRepository,
                              LedgerEntryRepository ledgerEntryRepository,
                              EventService eventService,
                              PermissionService permissionService) {
        this.eventMemberRepository = eventMemberRepository;
        this.userRepository = userRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.eventService = eventService;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public List<EventMemberResponse> list(Long userId, Long eventId) {
        eventService.getViewableEvent(userId, eventId);
        return eventMemberRepository.findByEventIdOrderByIdAsc(eventId).stream()
                .map(EventMemberService::toResponse)
                .toList();
    }

    @Transactional
    public EventMemberResponse add(Long userId, Long eventId, EventMemberRequest request) {
        Event event = requireManageable(userId, eventId);
        EventMember member = new EventMember();
        member.setEvent(event);
        apply(member, request);
        return toResponse(eventMemberRepository.save(member));
    }

    @Transactional
    public EventMemberResponse update(Long userId, Long eventId, Long memberId, EventMemberRequest request) {
        requireManageable(userId, eventId);
        EventMember member = requireMember(eventId, memberId);
        apply(member, request);
        return toResponse(eventMemberRepository.save(member));
    }

    @Transactional
    public void remove(Long userId, Long eventId, Long memberId) {
        requireManageable(userId, eventId);
        EventMember member = requireMember(eventId, memberId);
        if (ledgerEntryRepository.existsByMemberId(memberId)) {
            throw new BadRequestException(
                    "Member has transactions and cannot be removed; delete their transactions first");
        }
        eventMemberRepository.delete(member);
    }

    /** Used by other services to resolve and validate a member belongs to the event. */
    @Transactional(readOnly = true)
    public EventMember requireMember(Long eventId, Long memberId) {
        EventMember member = eventMemberRepository.findById(memberId)
                .orElseThrow(() -> NotFoundException.of("Event member", memberId));
        if (!member.getEvent().getId().equals(eventId)) {
            throw new BadRequestException("Member " + memberId + " does not belong to this event");
        }
        return member;
    }

    private Event requireManageable(Long userId, Long eventId) {
        Event event = eventService.getViewableEvent(userId, eventId);
        permissionService.require(event.getWorkspace().getId(), userId, Action.MANAGE_EVENT);
        return event;
    }

    private void apply(EventMember member, EventMemberRequest request) {
        member.setDisplayName(request.displayName().trim());
        member.setEmail(request.email());
        member.setPhone(request.phone());
        if (request.userId() != null) {
            User user = userRepository.findById(request.userId())
                    .orElseThrow(() -> NotFoundException.of("User", request.userId()));
            member.setUser(user);
        } else {
            member.setUser(null);
        }
    }

    private static EventMemberResponse toResponse(EventMember m) {
        return new EventMemberResponse(
                m.getId(),
                m.getDisplayName(),
                m.getEmail(),
                m.getPhone(),
                m.getUser() != null ? m.getUser().getId() : null);
    }
}
