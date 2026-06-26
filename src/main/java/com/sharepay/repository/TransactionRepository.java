package com.sharepay.repository;

import com.sharepay.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByEventIdOrderByDateDescIdDesc(Long eventId);
}
