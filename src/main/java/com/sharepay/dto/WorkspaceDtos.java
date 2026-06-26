package com.sharepay.dto;

import com.sharepay.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class WorkspaceDtos {

    private WorkspaceDtos() {
    }

    public record WorkspaceRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 2000) String description) {
    }

    public record WorkspaceResponse(
            Long id,
            String name,
            String description,
            Role role,
            long memberCount,
            long eventCount,
            LocalDateTime createdAt) {
    }

    public record AddMemberRequest(
            @NotBlank @Email String email,
            @NotNull Role role) {
    }

    public record UpdateRoleRequest(@NotNull Role role) {
    }

    public record WorkspaceMemberResponse(
            Long id,
            Long userId,
            String email,
            String displayName,
            Role role) {
    }
}
