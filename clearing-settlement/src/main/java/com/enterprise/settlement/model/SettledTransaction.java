package com.enterprise.settlement.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * An append-only record of an authorized transaction consumed from Kafka,
 * awaiting inclusion in a settlement batch. In a real system this would be
 * populated by consuming the core engine's `transactions.authorized` topic
 * continuously; SettlementListener does exactly that here.
 */
@Entity
@Table(name = "settled_transactions", uniqueConstraints = @UniqueConstraint(name = "uk_settled_transaction_id", columnNames = "transactionId"))
public class SettledTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private BigDecimal amount;

    private String settlementBatchId;

    @Column(nullable = false)
    private Instant recordedAt = Instant.now();

    protected SettledTransaction() {
        // JPA
    }

    public SettledTransaction(String transactionId, String accountId, String merchantId, BigDecimal amount) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.merchantId = merchantId;
        this.amount = amount;
    }

    public String getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public String getAccountId() { return accountId; }
    public String getMerchantId() { return merchantId; }
    public BigDecimal getAmount() { return amount; }
    public String getSettlementBatchId() { return settlementBatchId; }
    public void setSettlementBatchId(String settlementBatchId) { this.settlementBatchId = settlementBatchId; }
    public Instant getRecordedAt() { return recordedAt; }
}
