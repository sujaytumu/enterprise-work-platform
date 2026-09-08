package com.enterprise.settlement.repository;

import com.enterprise.settlement.model.SettlementBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, String> {
}
