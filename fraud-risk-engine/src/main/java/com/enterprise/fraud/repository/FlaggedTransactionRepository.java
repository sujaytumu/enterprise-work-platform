package com.enterprise.fraud.repository;

import com.enterprise.fraud.model.FlaggedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlaggedTransactionRepository extends JpaRepository<FlaggedTransaction, String> {
    List<FlaggedTransaction> findByReviewStatus(FlaggedTransaction.ReviewStatus status);
    List<FlaggedTransaction> findByAccountId(String accountId);
}
