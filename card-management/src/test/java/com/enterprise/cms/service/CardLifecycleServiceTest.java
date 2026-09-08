package com.enterprise.cms.service;

import com.enterprise.cms.model.Card;
import com.enterprise.cms.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CardLifecycleServiceTest {

    private CardRepository cardRepository;
    private VaultClient vaultClient;
    private CardLifecycleService service;

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        vaultClient = mock(VaultClient.class);
        service = new CardLifecycleService(cardRepository, vaultClient);
        when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Card cardWithStatus(Card.CardStatus status) throws Exception {
        Card card = new Card("cust_1", "tok_abc", "1111", Card.CardType.VIRTUAL);
        card.setStatus(status);
        Field idField = Card.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(card, "card_1");
        return card;
    }

    @Test
    void issueCallsVaultAndCreatesIssuedCard() {
        when(vaultClient.issueCardToken("411111"))
                .thenReturn(new VaultClient.IssuedCardToken("tok_xyz", "9999"));

        Card card = service.issueCard("cust_1", Card.CardType.VIRTUAL, "411111");

        assertEquals(Card.CardStatus.ISSUED, card.getStatus());
        assertEquals("tok_xyz", card.getCardToken());
        assertEquals("9999", card.getLastFour());
    }

    @Test
    void activateFromIssuedSucceeds() throws Exception {
        Card card = cardWithStatus(Card.CardStatus.ISSUED);
        when(cardRepository.findById("card_1")).thenReturn(java.util.Optional.of(card));

        Card activated = service.activate("card_1");

        assertEquals(Card.CardStatus.ACTIVE, activated.getStatus());
        assertNotNull(activated.getActivatedAt());
    }

    @Test
    void activateFromActiveThrows() throws Exception {
        Card card = cardWithStatus(Card.CardStatus.ACTIVE);
        when(cardRepository.findById("card_1")).thenReturn(java.util.Optional.of(card));

        assertThrows(IllegalStateException.class, () -> service.activate("card_1"));
    }

    @Test
    void setPinOnActiveCardStoresOnlyHash() throws Exception {
        Card card = cardWithStatus(Card.CardStatus.ACTIVE);
        when(cardRepository.findById("card_1")).thenReturn(java.util.Optional.of(card));

        Card updated = service.setPin("card_1", "1234");

        assertNotNull(updated.getPinHash());
        assertNotEquals("1234", updated.getPinHash());
    }

    @Test
    void closedCardCannotBeBlocked() throws Exception {
        Card card = cardWithStatus(Card.CardStatus.CLOSED);
        when(cardRepository.findById("card_1")).thenReturn(java.util.Optional.of(card));

        assertThrows(IllegalStateException.class, () -> service.block("card_1"));
    }

    @Test
    void blockedCardCanBeUnblockedBackToActive() throws Exception {
        Card card = cardWithStatus(Card.CardStatus.BLOCKED);
        when(cardRepository.findById("card_1")).thenReturn(java.util.Optional.of(card));

        Card unblocked = service.unblock("card_1");

        assertEquals(Card.CardStatus.ACTIVE, unblocked.getStatus());
    }
}
