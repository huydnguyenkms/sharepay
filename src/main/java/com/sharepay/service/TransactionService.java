package com.sharepay.service;

import com.sharepay.common.MoneyUtil;
import com.sharepay.domain.Category;
import com.sharepay.domain.Event;
import com.sharepay.domain.EventMember;
import com.sharepay.domain.Receipt;
import com.sharepay.domain.Transaction;
import com.sharepay.domain.TransactionPayer;
import com.sharepay.domain.TransactionSplit;
import com.sharepay.domain.TransactionSponsor;
import com.sharepay.domain.User;
import com.sharepay.domain.enums.SplitMethod;
import com.sharepay.domain.enums.TransactionType;
import com.sharepay.dto.TransactionDtos.CategoryRef;
import com.sharepay.dto.TransactionDtos.CreateAdjustmentRequest;
import com.sharepay.dto.TransactionDtos.CreateExpenseRequest;
import com.sharepay.dto.TransactionDtos.CreateRefundRequest;
import com.sharepay.dto.TransactionDtos.MemberAmount;
import com.sharepay.dto.TransactionDtos.MemberShare;
import com.sharepay.dto.TransactionDtos.ParticipantInput;
import com.sharepay.dto.TransactionDtos.PayerInput;
import com.sharepay.dto.TransactionDtos.ReceiptRef;
import com.sharepay.dto.TransactionDtos.SponsorInput;
import com.sharepay.dto.TransactionDtos.TransactionResponse;
import com.sharepay.exception.BadRequestException;
import com.sharepay.exception.NotFoundException;
import com.sharepay.ledger.LedgerService;
import com.sharepay.repository.CategoryRepository;
import com.sharepay.repository.ReceiptRepository;
import com.sharepay.repository.TransactionRepository;
import com.sharepay.repository.UserRepository;
import com.sharepay.service.PermissionService.Action;
import com.sharepay.split.SplitInput;
import com.sharepay.split.SplitStrategyFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final ReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final EventMemberService eventMemberService;
    private final PermissionService permissionService;
    private final SplitStrategyFactory splitStrategyFactory;
    private final LedgerService ledgerService;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository,
                              ReceiptRepository receiptRepository,
                              UserRepository userRepository,
                              EventService eventService,
                              EventMemberService eventMemberService,
                              PermissionService permissionService,
                              SplitStrategyFactory splitStrategyFactory,
                              LedgerService ledgerService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.receiptRepository = receiptRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.eventMemberService = eventMemberService;
        this.permissionService = permissionService;
        this.splitStrategyFactory = splitStrategyFactory;
        this.ledgerService = ledgerService;
    }

    // --- Create ---

    @Transactional
    public TransactionResponse createExpense(Long userId, Long eventId, CreateExpenseRequest request) {
        Event event = requireAddable(userId, eventId);
        String currency = event.getCurrency();
        BigDecimal amount = MoneyUtil.round(request.amount(), currency);

        Transaction tx = newTransaction(userId, event, TransactionType.EXPENSE, request.title(),
                request.categoryId(), amount, request.date(), request.description());
        tx.setSplitMethod(request.splitMethod());

        // Payers (credit side) must cover the full expense amount.
        BigDecimal payerSum = MoneyUtil.zero(currency);
        for (PayerInput p : request.payers()) {
            EventMember member = eventMemberService.requireMember(eventId, p.memberId());
            BigDecimal a = MoneyUtil.round(p.amount(), currency);
            tx.addPayer(new TransactionPayer(member, a));
            payerSum = payerSum.add(a);
        }
        if (payerSum.compareTo(amount) != 0) {
            throw new BadRequestException("Sum of payers (" + payerSum + ") must equal the amount (" + amount + ")");
        }

        // Sponsors reduce the shareable amount.
        BigDecimal sponsoredSum = MoneyUtil.zero(currency);
        if (request.sponsors() != null) {
            for (SponsorInput s : request.sponsors()) {
                EventMember member = eventMemberService.requireMember(eventId, s.memberId());
                BigDecimal a = MoneyUtil.round(s.amount(), currency);
                tx.addSponsor(new TransactionSponsor(member, a));
                sponsoredSum = sponsoredSum.add(a);
            }
        }
        if (sponsoredSum.compareTo(amount) > 0) {
            throw new BadRequestException("Sponsored amount cannot exceed the expense amount");
        }

        BigDecimal net = amount.subtract(sponsoredSum);
        applySplits(tx, eventId, request.splitMethod(), request.participants(), net, currency);

        tx = transactionRepository.save(tx);
        ledgerService.repost(tx);
        return toResponse(tx);
    }

    @Transactional
    public TransactionResponse createRefund(Long userId, Long eventId, CreateRefundRequest request) {
        Event event = requireAddable(userId, eventId);
        String currency = event.getCurrency();
        BigDecimal amount = MoneyUtil.round(request.amount(), currency);

        Transaction tx = newTransaction(userId, event, TransactionType.REFUND, request.title(),
                request.categoryId(), amount, request.date(), request.description());
        tx.setSplitMethod(SplitMethod.EQUAL);

        // Debit side: the member who held the returned cash.
        EventMember receiver = eventMemberService.requireMember(eventId, request.receiverMemberId());
        tx.addSplit(new TransactionSplit(receiver, amount, null));

        // Credit side: beneficiaries share the refund equally.
        List<SplitInput> inputs = request.beneficiaryMemberIds().stream()
                .map(id -> {
                    eventMemberService.requireMember(eventId, id);
                    return new SplitInput(id, null);
                })
                .toList();
        Map<Long, BigDecimal> credits = splitStrategyFactory.forMethod(SplitMethod.EQUAL)
                .computeShares(amount, inputs, currency);
        for (Map.Entry<Long, BigDecimal> entry : credits.entrySet()) {
            EventMember beneficiary = eventMemberService.requireMember(eventId, entry.getKey());
            tx.addPayer(new TransactionPayer(beneficiary, entry.getValue()));
        }

        Transaction saved = transactionRepository.save(tx);
        ledgerService.repost(saved);
        return toResponse(saved);
    }

    @Transactional
    public TransactionResponse createAdjustment(Long userId, Long eventId, CreateAdjustmentRequest request) {
        Event event = requireAddable(userId, eventId);
        String currency = event.getCurrency();
        BigDecimal amount = MoneyUtil.round(request.amount(), currency);

        if (request.debitMemberId().equals(request.creditMemberId())) {
            throw new BadRequestException("Adjustment must move money between two different members");
        }
        EventMember credited = eventMemberService.requireMember(eventId, request.creditMemberId());
        EventMember debited = eventMemberService.requireMember(eventId, request.debitMemberId());

        Transaction tx = newTransaction(userId, event, TransactionType.ADJUSTMENT, request.title(),
                null, amount, request.date(), request.description());
        tx.addPayer(new TransactionPayer(credited, amount)); // credit side
        tx.addSplit(new TransactionSplit(debited, amount, null)); // debit side

        tx = transactionRepository.save(tx);
        ledgerService.repost(tx);
        return toResponse(tx);
    }

    @Transactional
    public TransactionResponse updateExpense(Long userId, Long eventId, Long txId, CreateExpenseRequest request) {
        Transaction existing = requireTransaction(eventId, txId);
        if (existing.getType() != TransactionType.EXPENSE) {
            throw new BadRequestException("Only expense transactions can be edited via this endpoint");
        }
        // Replace by deleting and recreating so ledger posting stays consistent.
        delete(userId, eventId, txId);
        return createExpense(userId, eventId, request);
    }

    // --- Read ---

    @Transactional(readOnly = true)
    public List<TransactionResponse> list(Long userId, Long eventId) {
        eventService.getViewableEvent(userId, eventId);
        return transactionRepository.findByEventIdOrderByDateDescIdDesc(eventId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(Long userId, Long eventId, Long txId) {
        eventService.getViewableEvent(userId, eventId);
        return toResponse(requireTransaction(eventId, txId));
    }

    // --- Delete ---

    @Transactional
    public void delete(Long userId, Long eventId, Long txId) {
        requireAddable(userId, eventId);
        Transaction tx = requireTransaction(eventId, txId);
        receiptRepository.deleteAll(receiptRepository.findByTransactionId(txId));
        ledgerService.remove(txId);
        transactionRepository.delete(tx);
    }

    // --- Receipts ---

    @Transactional
    public ReceiptRef uploadReceipt(Long userId, Long eventId, Long txId, MultipartFile file) {
        requireAddable(userId, eventId);
        Transaction tx = requireTransaction(eventId, txId);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Receipt file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.startsWith("image/") || contentType.equals("application/pdf"))) {
            throw new BadRequestException("Receipt must be a JPG, PNG or PDF");
        }
        Receipt receipt = new Receipt();
        receipt.setTransaction(tx);
        receipt.setFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "receipt");
        receipt.setContentType(contentType);
        receipt.setSizeBytes(file.getSize());
        try {
            receipt.setData(file.getBytes());
        } catch (IOException e) {
            throw new BadRequestException("Could not read uploaded file");
        }
        receipt = receiptRepository.save(receipt);
        return new ReceiptRef(receipt.getId(), receipt.getFileName(), receipt.getContentType(), receipt.getSizeBytes());
    }

    @Transactional(readOnly = true)
    public Receipt getReceipt(Long userId, Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> NotFoundException.of("Receipt", receiptId));
        Event event = receipt.getTransaction().getEvent();
        permissionService.require(event.getWorkspace().getId(), userId, Action.VIEW);
        return receipt;
    }

    // --- helpers ---

    private void applySplits(Transaction tx, Long eventId, SplitMethod method,
                             List<ParticipantInput> participants, BigDecimal net, String currency) {
        List<SplitInput> inputs = participants.stream()
                .map(p -> {
                    eventMemberService.requireMember(eventId, p.memberId());
                    return new SplitInput(p.memberId(), p.value());
                })
                .toList();
        Map<Long, BigDecimal> shares = splitStrategyFactory.forMethod(method).computeShares(net, inputs, currency);

        Map<Long, BigDecimal> inputByMember = new java.util.HashMap<>();
        participants.forEach(p -> inputByMember.put(p.memberId(), p.value()));

        shares.forEach((memberId, share) -> {
            EventMember member = eventMemberService.requireMember(eventId, memberId);
            BigDecimal inputValue = method == SplitMethod.EQUAL ? null : inputByMember.get(memberId);
            tx.addSplit(new TransactionSplit(member, share, inputValue));
        });
    }

    private Transaction newTransaction(Long userId, Event event, TransactionType type, String title,
                                       Long categoryId, BigDecimal amount, java.time.LocalDate date, String description) {
        Transaction tx = new Transaction();
        tx.setEvent(event);
        tx.setType(type);
        tx.setTitle(title.trim());
        tx.setCategory(resolveCategory(event, categoryId));
        tx.setAmount(amount);
        tx.setDate(date);
        tx.setDescription(description);
        User creator = userRepository.findById(userId).orElse(null);
        tx.setCreatedBy(creator);
        return tx;
    }

    private Category resolveCategory(Event event, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> NotFoundException.of("Category", categoryId));
        boolean isDefault = category.getEvent() == null;
        boolean belongsToEvent = category.getEvent() != null && category.getEvent().getId().equals(event.getId());
        if (!isDefault && !belongsToEvent) {
            throw new BadRequestException("Category does not belong to this event");
        }
        return category;
    }

    private Transaction requireTransaction(Long eventId, Long txId) {
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> NotFoundException.of("Transaction", txId));
        if (!tx.getEvent().getId().equals(eventId)) {
            throw NotFoundException.of("Transaction", txId);
        }
        return tx;
    }

    private Event requireAddable(Long userId, Long eventId) {
        Event event = eventService.getViewableEvent(userId, eventId);
        permissionService.require(event.getWorkspace().getId(), userId, Action.ADD_TRANSACTION);
        return event;
    }

    private TransactionResponse toResponse(Transaction tx) {
        CategoryRef categoryRef = tx.getCategory() == null ? null
                : new CategoryRef(tx.getCategory().getId(), tx.getCategory().getName());

        List<MemberAmount> payers = tx.getPayers().stream()
                .map(p -> new MemberAmount(p.getMember().getId(), p.getMember().getDisplayName(), p.getAmount()))
                .toList();
        List<MemberShare> splits = tx.getSplits().stream()
                .map(s -> new MemberShare(s.getMember().getId(), s.getMember().getDisplayName(),
                        s.getShareAmount(), s.getInputValue()))
                .toList();
        List<MemberAmount> sponsors = tx.getSponsors().stream()
                .map(s -> new MemberAmount(s.getMember().getId(), s.getMember().getDisplayName(), s.getAmount()))
                .toList();
        List<ReceiptRef> receipts = receiptRepository.findByTransactionId(tx.getId()).stream()
                .map(r -> new ReceiptRef(r.getId(), r.getFileName(), r.getContentType(), r.getSizeBytes()))
                .toList();

        return new TransactionResponse(
                tx.getId(),
                tx.getType(),
                tx.getTitle(),
                categoryRef,
                tx.getAmount(),
                tx.getDate(),
                tx.getDescription(),
                tx.getSplitMethod(),
                payers,
                splits,
                sponsors,
                receipts,
                tx.getCreatedAt());
    }
}
