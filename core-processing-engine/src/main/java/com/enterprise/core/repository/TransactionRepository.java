package com.enterprise.core.repository;

import com.enterprise.core.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByAccountIdAndCreatedAtAfter(String accountId, Instant since);
}
