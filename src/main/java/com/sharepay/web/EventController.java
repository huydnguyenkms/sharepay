package com.sharepay.web;

import com.sharepay.domain.enums.EventStatus;
import com.sharepay.dto.EventDtos.DuplicateEventRequest;
import com.sharepay.dto.EventDtos.EventRequest;
import com.sharepay.dto.EventDtos.EventResponse;
import com.sharepay.dto.EventDtos.UpdateStatusRequest;
import com.sharepay.security.AppUserPrincipal;
import com.sharepay.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/api/workspaces/{workspaceId}/events")
    public List<EventResponse> list(@AuthenticationPrincipal AppUserPrincipal principal,
                                    @PathVariable Long workspaceId,
                                    @RequestParam(required = false) EventStatus status) {
        return eventService.list(principal.getId(), workspaceId, status);
    }

    @PostMapping("/api/workspaces/{workspaceId}/events")
    public ResponseEntity<EventResponse> create(@AuthenticationPrincipal AppUserPrincipal principal,
                                                @PathVariable Long workspaceId,
                                                @Valid @RequestBody EventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.create(principal.getId(), workspaceId, request));
    }

    @GetMapping("/api/events/{eventId}")
    public EventResponse get(@AuthenticationPrincipal AppUserPrincipal principal,
                             @PathVariable Long eventId) {
        return eventService.get(principal.getId(), eventId);
    }

    @PutMapping("/api/events/{eventId}")
    public EventResponse update(@AuthenticationPrincipal AppUserPrincipal principal,
                                @PathVariable Long eventId,
                                @Valid @RequestBody EventRequest request) {
        return eventService.update(principal.getId(), eventId, request);
    }

    @PatchMapping("/api/events/{eventId}/status")
    public EventResponse updateStatus(@AuthenticationPrincipal AppUserPrincipal principal,
                                      @PathVariable Long eventId,
                                      @Valid @RequestBody UpdateStatusRequest request) {
        return eventService.updateStatus(principal.getId(), eventId, request);
    }

    @PostMapping("/api/events/{eventId}/duplicate")
    public ResponseEntity<EventResponse> duplicate(@AuthenticationPrincipal AppUserPrincipal principal,
                                                   @PathVariable Long eventId,
                                                   @Valid @RequestBody DuplicateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.duplicate(principal.getId(), eventId, request));
    }

    @DeleteMapping("/api/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserPrincipal principal,
                       @PathVariable Long eventId) {
        eventService.delete(principal.getId(), eventId);
    }
}
