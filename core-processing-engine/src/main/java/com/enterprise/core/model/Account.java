package com.enterprise.core.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * In a real system this would be a vault-issued token, never a raw PAN.
     * Kept as a demo field here; see docs/ARCHITECTURE.md for the planned
     * Tokenization Vault phase.
     */
    @Column(nullable = false, unique = true)
    private String cardToken;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private BigDecimal dailyLimit;

    @Column(nullable = false)
    private BigDecimal spentToday = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public enum AccountStatus {
        ACTIVE, BLOCKED, CLOSED
    }

    protected Account() {
        // JPA
    }

    public Account(String cardToken, BigDecimal balance, BigDecimal dailyLimit) {
        this.cardToken = cardToken;
        this.balance = balance;
        this.dailyLimit = dailyLimit;
    }

    public String getId() { return id; }
    public String getCardToken() { return cardToken; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public BigDecimal getSpentToday() { return spentToday; }
    public void setSpentToday(BigDecimal spentToday) { this.spentToday = spentToday; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
