package com.enterprise.fraud.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "flagged_transactions")
public class FlaggedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private double riskScore;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    @Column(nullable = false)
    private Instant flaggedAt = Instant.now();

    public enum ReviewStatus {
        PENDING, CONFIRMED_FRAUD, FALSE_POSITIVE
    }

    protected FlaggedTransaction() {
        // JPA
    }

    public FlaggedTransaction(String transactionId, String accountId, BigDecimal amount, double riskScore, String reason) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amount = amount;
        this.riskScore = riskScore;
        this.reason = reason;
    }

    public String getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public String getAccountId() { return accountId; }
    public BigDecimal getAmount() { return amount; }
    public double getRiskScore() { return riskScore; }
    public String getReason() { return reason; }
    public ReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(ReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }
    public Instant getFlaggedAt() { return flaggedAt; }
}
