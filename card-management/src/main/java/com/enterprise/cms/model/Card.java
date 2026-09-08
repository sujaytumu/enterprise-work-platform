package com.enterprise.cms.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Represents a card's lifecycle in the CMS. Note: `cardToken` here refers to
 * the token issued by tokenization-vault, NOT the raw PAN — the CMS never
 * stores or sees the raw card number; it calls the vault service to
 * generate one at issuance time and only ever holds the resulting token.
 */
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false, unique = true)
    private String cardToken;

    @Column(nullable = false, length = 4)
    private String lastFour;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType cardType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status = CardStatus.ISSUED;

    /** SHA-256 hash of the PIN. The plaintext PIN is never stored, ever. */
    private String pinHash;

    @Column(nullable = false)
    private Instant issuedAt = Instant.now();

    private Instant activatedAt;
    private Instant blockedAt;
    private Instant closedAt;

    public enum CardType {
        VIRTUAL, PHYSICAL
    }

    /**
     * Valid transitions: ISSUED -> ACTIVE -> (BLOCKED <-> ACTIVE) -> CLOSED.
     * Enforced in CardLifecycleService, not here — this enum is just the
     * state set.
     */
    public enum CardStatus {
        ISSUED, ACTIVE, BLOCKED, CLOSED
    }

    protected Card() {
        // JPA
    }

    public Card(String customerId, String cardToken, String lastFour, CardType cardType) {
        this.customerId = customerId;
        this.cardToken = cardToken;
        this.lastFour = lastFour;
        this.cardType = cardType;
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getCardToken() { return cardToken; }
    public String getLastFour() { return lastFour; }
    public CardType getCardType() { return cardType; }
    public CardStatus getStatus() { return status; }
    public void setStatus(CardStatus status) { this.status = status; }
    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }
    public Instant getBlockedAt() { return blockedAt; }
    public void setBlockedAt(Instant blockedAt) { this.blockedAt = blockedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
