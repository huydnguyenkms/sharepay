package com.sharepay.repository;

import com.sharepay.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByEventId(Long eventId);

    boolean existsByMemberId(Long eventMemberId);

    @Modifying
    @Query("delete from LedgerEntry e where e.transaction.id = :transactionId")
    void deleteByTransactionId(@Param("transactionId") Long transactionId);
}
