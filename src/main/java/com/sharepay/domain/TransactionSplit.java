package com.sharepay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * A participant's resolved share of a transaction. {@code shareAmount} is the computed
 * money owed; {@code inputValue} holds the user's raw input for EXACT (amount),
 * PERCENTAGE (percent) or WEIGHT (weight) split methods, and is null for EQUAL.
 */
@Entity
@Table(name = "transaction_splits")
public class TransactionSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_member_id", nullable = false)
    private EventMember member;

    @Column(name = "share_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal shareAmount;

    @Column(name = "input_value", precision = 19, scale = 4)
    private BigDecimal inputValue;

    public TransactionSplit() {
    }

    public TransactionSplit(EventMember member, BigDecimal shareAmount, BigDecimal inputValue) {
        this.member = member;
        this.shareAmount = shareAmount;
        this.inputValue = inputValue;
    }

    public Long getId() {
        return id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public EventMember getMember() {
        return member;
    }

    public void setMember(EventMember member) {
        this.member = member;
    }

    public BigDecimal getShareAmount() {
        return shareAmount;
    }

    public void setShareAmount(BigDecimal shareAmount) {
        this.shareAmount = shareAmount;
    }

    public BigDecimal getInputValue() {
        return inputValue;
    }

    public void setInputValue(BigDecimal inputValue) {
        this.inputValue = inputValue;
    }
}
