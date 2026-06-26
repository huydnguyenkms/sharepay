package com.sharepay.web;

import com.sharepay.dto.MemberDtos.EventMemberRequest;
import com.sharepay.dto.MemberDtos.EventMemberResponse;
import com.sharepay.security.AppUserPrincipal;
import com.sharepay.service.EventMemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/members")
public class EventMemberController {

    private final EventMemberService eventMemberService;

    public EventMemberController(EventMemberService eventMemberService) {
        this.eventMemberService = eventMemberService;
    }

    @GetMapping
    public List<EventMemberResponse> list(@AuthenticationPrincipal AppUserPrincipal principal,
                                          @PathVariable Long eventId) {
        return eventMemberService.list(principal.getId(), eventId);
    }

    @PostMapping
    public ResponseEntity<EventMemberResponse> add(@AuthenticationPrincipal AppUserPrincipal principal,
                                                   @PathVariable Long eventId,
                                                   @Valid @RequestBody EventMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventMemberService.add(principal.getId(), eventId, request));
    }

    @PutMapping("/{memberId}")
    public EventMemberResponse update(@AuthenticationPrincipal AppUserPrincipal principal,
                                      @PathVariable Long eventId,
                                      @PathVariable Long memberId,
                                      @Valid @RequestBody EventMemberRequest request) {
        return eventMemberService.update(principal.getId(), eventId, memberId, request);
    }

    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal AppUserPrincipal principal,
                       @PathVariable Long eventId,
                       @PathVariable Long memberId) {
        eventMemberService.remove(principal.getId(), eventId, memberId);
    }
}
