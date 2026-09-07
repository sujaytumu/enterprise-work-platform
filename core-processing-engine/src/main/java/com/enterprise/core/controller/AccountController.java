package com.enterprise.core.controller;

import com.enterprise.core.model.Account;
import com.enterprise.core.repository.AccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Convenience endpoints for local/demo use only — creating and inspecting
 * accounts with synthetic data. A real system provisions accounts via the
 * Card Management System (planned next phase), not a raw REST POST.
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public record CreateAccountRequest(String cardToken, BigDecimal balance, BigDecimal dailyLimit) {}

    @PostMapping
    public ResponseEntity<Account> create(@RequestBody CreateAccountRequest request) {
        Account account = new Account(request.cardToken(), request.balance(), request.dailyLimit());
        return ResponseEntity.ok(accountRepository.save(account));
    }

    @GetMapping("/{cardToken}")
    public ResponseEntity<?> get(@PathVariable String cardToken) {
        return accountRepository.findByCardToken(cardToken)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
