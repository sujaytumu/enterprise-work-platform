package com.enterprise.cms.service;

import com.enterprise.cms.model.Card;
import com.enterprise.cms.repository.CardRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Enforces card lifecycle transitions: ISSUED -> ACTIVE -> BLOCKED/CLOSED,
 * with BLOCKED <-> ACTIVE allowed (e.g. customer reports lost card, then
 * finds it and unblocks), but nothing can leave CLOSED once reached.
 */
@Service
public class CardLifecycleService {

    private final CardRepository cardRepository;
    private final VaultClient vaultClient;
    private final BCryptPasswordEncoder pinEncoder = new BCryptPasswordEncoder();

    public CardLifecycleService(CardRepository cardRepository, VaultClient vaultClient) {
        this.cardRepository = cardRepository;
        this.vaultClient = vaultClient;
    }

    public Card issueCard(String customerId, Card.CardType cardType, String binPrefix) {
        VaultClient.IssuedCardToken issued = vaultClient.issueCardToken(binPrefix);
        Card card = new Card(customerId, issued.token(), issued.lastFour(), cardType);
        return cardRepository.save(card);
    }

    public Card activate(String cardId) {
        Card card = get(cardId);
        requireStatus(card, Card.CardStatus.ISSUED, "activate");
        card.setStatus(Card.CardStatus.ACTIVE);
        card.setActivatedAt(java.time.Instant.now());
        return cardRepository.save(card);
    }

    /** Sets the PIN. Only the bcrypt hash is ever persisted — the plaintext PIN exists only for this call. */
    public Card setPin(String cardId, String pin) {
        if (pin == null || !pin.matches("\\d{4,6}")) {
            throw new IllegalArgumentException("PIN must be 4-6 digits");
        }
        Card card = get(cardId);
        requireStatus(card, Card.CardStatus.ACTIVE, "set PIN on");
        card.setPinHash(pinEncoder.encode(pin));
        return cardRepository.save(card);
    }

    public Card block(String cardId) {
        Card card = get(cardId);
        if (card.getStatus() == Card.CardStatus.CLOSED) {
            throw new IllegalStateException("Cannot block a closed card");
        }
        card.setStatus(Card.CardStatus.BLOCKED);
        card.setBlockedAt(java.time.Instant.now());
        return cardRepository.save(card);
    }

    public Card unblock(String cardId) {
        Card card = get(cardId);
        requireStatus(card, Card.CardStatus.BLOCKED, "unblock");
        card.setStatus(Card.CardStatus.ACTIVE);
        return cardRepository.save(card);
    }

    public Card close(String cardId) {
        Card card = get(cardId);
        if (card.getStatus() == Card.CardStatus.CLOSED) {
            throw new IllegalStateException("Card is already closed");
        }
        card.setStatus(Card.CardStatus.CLOSED);
        card.setClosedAt(java.time.Instant.now());
        return cardRepository.save(card);
    }

    private Card get(String cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("No card found: " + cardId));
    }

    private void requireStatus(Card card, Card.CardStatus required, String action) {
        if (card.getStatus() != required) {
            throw new IllegalStateException(
                    "Cannot " + action + " a card in status " + card.getStatus() + " (requires " + required + ")");
        }
    }
}
