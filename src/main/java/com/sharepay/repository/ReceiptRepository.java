package com.sharepay.repository;

import com.sharepay.domain.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    List<Receipt> findByTransactionId(Long transactionId);
}
