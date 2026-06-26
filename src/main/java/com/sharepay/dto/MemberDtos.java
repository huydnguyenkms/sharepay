package com.sharepay.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class MemberDtos {

    private MemberDtos() {
    }

    public record EventMemberRequest(
            @NotBlank @Size(max = 255) String displayName,
            @Email @Size(max = 255) String email,
            @Size(max = 50) String phone,
            Long userId) {
    }

    public record EventMemberResponse(
            Long id,
            String displayName,
            String email,
            String phone,
            Long userId) {
    }

    /** A distinct participant seen across a workspace's events, for reuse when adding members. */
    public record KnownMemberResponse(
            String displayName,
            String email,
            String phone,
            Long userId) {
    }

    /** Member position within an event, as shown on the Members screen. */
    public record MemberSummaryResponse(
            Long memberId,
            String displayName,
            BigDecimal paid,
            BigDecimal owed,
            BigDecimal sponsored,
            BigDecimal balance) {
    }
}
