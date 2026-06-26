package com.sharepay.dto;

import com.sharepay.domain.enums.EventStatus;
import com.sharepay.domain.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class EventDtos {

    private EventDtos() {
    }

    public record EventRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 2000) String description,
            @NotNull EventType type,
            @NotBlank @Size(min = 3, max = 3) String currency,
            LocalDate startDate,
            LocalDate endDate) {
    }

    public record UpdateStatusRequest(@NotNull EventStatus status) {
    }

    public record DuplicateEventRequest(
            @Size(max = 255) String name,
            boolean copyMembers) {
    }

    public record EventResponse(
            Long id,
            Long workspaceId,
            String name,
            String description,
            EventType type,
            String currency,
            LocalDate startDate,
            LocalDate endDate,
            EventStatus status,
            long memberCount,
            LocalDateTime settledAt,
            LocalDateTime createdAt) {
    }
}
