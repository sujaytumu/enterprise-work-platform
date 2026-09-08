package com.enterprise.settlement.repository;

import com.enterprise.settlement.model.SettledTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettledTransactionRepository extends JpaRepository<SettledTransaction, String> {
    List<SettledTransaction> findBySettlementBatchIdIsNull();
}
