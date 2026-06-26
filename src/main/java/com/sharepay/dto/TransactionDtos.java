package com.sharepay.dto;

import com.sharepay.domain.enums.SplitMethod;
import com.sharepay.domain.enums.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class TransactionDtos {

    private TransactionDtos() {
    }

    public record PayerInput(
            @NotNull Long memberId,
            @NotNull @Positive BigDecimal amount) {
    }

    /** value: ignored for EQUAL, exact money for EXACT, percent for PERCENTAGE, weight for WEIGHT. */
    public record ParticipantInput(
            @NotNull Long memberId,
            @PositiveOrZero BigDecimal value) {
    }

    public record SponsorInput(
            @NotNull Long memberId,
            @NotNull @Positive BigDecimal amount) {
    }

    /** Creates an EXPENSE. Sum of payers must equal {@code amount}; sponsors reduce the
     *  shareable amount split among participants. */
    public record CreateExpenseRequest(
            @NotBlank @Size(max = 255) String title,
            Long categoryId,
            @NotNull @Positive BigDecimal amount,
            @NotNull LocalDate date,
            @Size(max = 2000) String description,
            @NotNull SplitMethod splitMethod,
            @NotEmpty @Valid List<PayerInput> payers,
            @NotEmpty @Valid List<ParticipantInput> participants,
            @Valid List<SponsorInput> sponsors) {
    }

    /** Creates a REFUND. {@code receiverMemberId} held the returned cash; the amount is
     *  credited back equally to the beneficiaries. */
    public record CreateRefundRequest(
            @NotBlank @Size(max = 255) String title,
            Long categoryId,
            @NotNull @Positive BigDecimal amount,
            @NotNull LocalDate date,
            @Size(max = 2000) String description,
            @NotNull Long receiverMemberId,
            @NotEmpty List<Long> beneficiaryMemberIds) {
    }

    /** Creates an ADJUSTMENT moving {@code amount} from one member to another. */
    public record CreateAdjustmentRequest(
            @NotBlank @Size(max = 255) String title,
            @NotNull @Positive BigDecimal amount,
            @NotNull LocalDate date,
            @Size(max = 2000) String description,
            @NotNull Long debitMemberId,
            @NotNull Long creditMemberId) {
    }

    public record CategoryRef(Long id, String name) {
    }

    public record MemberAmount(Long memberId, String displayName, BigDecimal amount) {
    }

    public record MemberShare(Long memberId, String displayName, BigDecimal shareAmount, BigDecimal inputValue) {
    }

    public record ReceiptRef(Long id, String fileName, String contentType, long sizeBytes) {
    }

    public record TransactionResponse(
            Long id,
            TransactionType type,
            String title,
            CategoryRef category,
            BigDecimal amount,
            LocalDate date,
            String description,
            SplitMethod splitMethod,
            List<MemberAmount> payers,
            List<MemberShare> splits,
            List<MemberAmount> sponsors,
            List<ReceiptRef> receipts,
            LocalDateTime createdAt) {
    }
}
