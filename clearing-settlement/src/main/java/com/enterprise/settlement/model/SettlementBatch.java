package com.enterprise.settlement.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "settlement_batches")
public class SettlementBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private int transactionCount;

    @Column(nullable = false)
    private Instant runAt = Instant.now();

    protected SettlementBatch() {
        // JPA
    }

    public SettlementBatch(BigDecimal totalAmount, int transactionCount) {
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
    }

    public String getId() { return id; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public int getTransactionCount() { return transactionCount; }
    public Instant getRunAt() { return runAt; }
}
