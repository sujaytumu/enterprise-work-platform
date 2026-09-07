package com.enterprise.core.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    private String declineReason;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public enum TransactionStatus {
        APPROVED, DECLINED
    }

    protected Transaction() {
        // JPA
    }

    public Transaction(String accountId, String merchantId, BigDecimal amount) {
        this.accountId = accountId;
        this.merchantId = merchantId;
        this.amount = amount;
    }

    public String getId() { return id; }
    public String getAccountId() { return accountId; }
    public String getMerchantId() { return merchantId; }
    public BigDecimal getAmount() { return amount; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getDeclineReason() { return declineReason; }
    public void setDeclineReason(String declineReason) { this.declineReason = declineReason; }
    public Instant getCreatedAt() { return createdAt; }
}
