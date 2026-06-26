package com.sharepay.ledger;

import com.sharepay.domain.Event;
import com.sharepay.domain.LedgerEntry;
import com.sharepay.domain.Transaction;
import com.sharepay.domain.enums.LedgerEntryType;
import com.sharepay.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * The single writer of ledger entries. Translates a transaction's payers / splits /
 * sponsors into balanced double-entry records and (re)posts them atomically.
 *
 * <p>Convention used across all transaction types:
 * <ul>
 *   <li>{@code payers} are the <b>credit</b> side (their balance increases),</li>
 *   <li>{@code splits} are the <b>debit</b> side (their balance decreases),</li>
 *   <li>{@code sponsors} are an additional sponsor-debit (expense only).</li>
 * </ul>
 * The transaction type selects which credit/debit subtype is posted. Because the caller
 * guarantees credit totals equal debit totals, every transaction is internally balanced
 * and the sum of all member balances in an event is always zero.
 */
@Service
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /** Deletes any prior entries for the transaction and posts a fresh, balanced set. */
    @Transactional
    public void repost(Transaction transaction) {
        if (transaction.getId() != null) {
            ledgerEntryRepository.deleteByTransactionId(transaction.getId());
            ledgerEntryRepository.flush();
        }
        ledgerEntryRepository.saveAll(build(transaction));
    }

    /** Removes all ledger entries produced by a transaction (used when it is deleted). */
    @Transactional
    public void remove(Long transactionId) {
        ledgerEntryRepository.deleteByTransactionId(transactionId);
    }

    private List<LedgerEntry> build(Transaction tx) {
        Event event = tx.getEvent();
        List<LedgerEntry> entries = new ArrayList<>();

        LedgerEntryType creditType;
        LedgerEntryType debitType;
        switch (tx.getType()) {
            case EXPENSE, SPONSOR -> {
                creditType = LedgerEntryType.PAYMENT_CREDIT;
                debitType = LedgerEntryType.SHARE_DEBIT;
                tx.getSponsors().forEach(s -> entries.add(
                        new LedgerEntry(event, s.getMember(), tx, LedgerEntryType.SPONSOR_DEBIT, s.getAmount())));
            }
            case REFUND -> {
                creditType = LedgerEntryType.REFUND_CREDIT;
                debitType = LedgerEntryType.REFUND_DEBIT;
            }
            case ADJUSTMENT -> {
                creditType = LedgerEntryType.ADJUSTMENT_CREDIT;
                debitType = LedgerEntryType.ADJUSTMENT_DEBIT;
            }
            default -> throw new IllegalStateException("Unsupported transaction type: " + tx.getType());
        }

        for (var payer : tx.getPayers()) {
            entries.add(new LedgerEntry(event, payer.getMember(), tx, creditType, payer.getAmount()));
        }
        for (var split : tx.getSplits()) {
            entries.add(new LedgerEntry(event, split.getMember(), tx, debitType, split.getShareAmount()));
        }
        return entries;
    }
}
