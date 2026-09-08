package com.enterprise.cms.controller;

import com.enterprise.cms.model.Card;
import com.enterprise.cms.repository.CardRepository;
import com.enterprise.cms.service.CardLifecycleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private final CardLifecycleService lifecycleService;
    private final CardRepository cardRepository;

    public CardController(CardLifecycleService lifecycleService, CardRepository cardRepository) {
        this.lifecycleService = lifecycleService;
        this.cardRepository = cardRepository;
    }

    public record IssueCardRequest(String customerId, Card.CardType cardType, String binPrefix) {}
    public record SetPinRequest(String pin) {}

    @PostMapping
    public ResponseEntity<Card> issue(@RequestBody IssueCardRequest request) {
        Card card = lifecycleService.issueCard(
                request.customerId(), request.cardType(),
                request.binPrefix() != null ? request.binPrefix() : "411111");
        return ResponseEntity.ok(card);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Card> get(@PathVariable String id) {
        return cardRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Card>> listByCustomer(@RequestParam String customerId) {
        return ResponseEntity.ok(cardRepository.findByCustomerId(customerId));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Card> activate(@PathVariable String id) {
        return ResponseEntity.ok(lifecycleService.activate(id));
    }

    @PostMapping("/{id}/pin")
    public ResponseEntity<Void> setPin(@PathVariable String id, @RequestBody SetPinRequest request) {
        lifecycleService.setPin(id, request.pin());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<Card> block(@PathVariable String id) {
        return ResponseEntity.ok(lifecycleService.block(id));
    }

    @PostMapping("/{id}/unblock")
    public ResponseEntity<Card> unblock(@PathVariable String id) {
        return ResponseEntity.ok(lifecycleService.unblock(id));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<Card> close(@PathVariable String id) {
        return ResponseEntity.ok(lifecycleService.close(id));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<String> handleErrors(RuntimeException e) {
        HttpStatus status = e instanceof IllegalArgumentException ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(e.getMessage());
    }
}
