package com.enterprise.core.service;

import com.enterprise.core.exception.AccountNotFoundException;
import com.enterprise.core.model.Account;
import com.enterprise.core.model.AuthorizationRequest;
import com.enterprise.core.model.AuthorizationResponse;
import com.enterprise.core.model.Transaction;
import com.enterprise.core.repository.AccountRepository;
import com.enterprise.core.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Core authorization decision logic. This is the path that in a real system
 * must complete in low-single-digit milliseconds; here the logic is
 * straightforward and synchronous, and Kafka publishing happens after the
 * decision is made so it never blocks the authorization response.
 */
@Service
public class AuthorizationService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final VelocityRuleService velocityRuleService;
    private final TransactionEventPublisher eventPublisher;

    public AuthorizationService(AccountRepository accountRepository,
                                 TransactionRepository transactionRepository,
                                 VelocityRuleService velocityRuleService,
                                 TransactionEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.velocityRuleService = velocityRuleService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AuthorizationResponse authorize(AuthorizationRequest request) {
        Account account = accountRepository.findWithLockByCardToken(request.getCardToken())
                .orElseThrow(() -> new AccountNotFoundException(request.getCardToken()));

        Transaction transaction = new Transaction(account.getId(), request.getMerchantId(), request.getAmount());

        Decision decision = evaluate(account, request.getAmount());

        if (decision.approved()) {
            account.setBalance(account.getBalance().subtract(request.getAmount()));
            account.setSpentToday(account.getSpentToday().add(request.getAmount()));
            accountRepository.save(account);
            transaction.setStatus(Transaction.TransactionStatus.APPROVED);
        } else {
            transaction.setStatus(Transaction.TransactionStatus.DECLINED);
            transaction.setDeclineReason(decision.reason());
        }

        transaction = transactionRepository.save(transaction);
        eventPublisher.publish(transaction);

        return new AuthorizationResponse(
                transaction.getId(),
                transaction.getStatus().name(),
                decision.responseCode(),
                decision.reason() == null ? "Approved" : decision.reason());
    }

    private Decision evaluate(Account account, BigDecimal amount) {
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            return Decision.decline("62", "Account is " + account.getStatus());
        }
        if (account.getBalance().compareTo(amount) < 0) {
            return Decision.decline("51", "Insufficient funds");
        }
        if (account.getSpentToday().add(amount).compareTo(account.getDailyLimit()) > 0) {
            return Decision.decline("61", "Exceeds daily limit");
        }

        VelocityRuleService.VelocityCheckResult velocity = velocityRuleService.check(account.getId());
        if (velocity.flagged()) {
            return Decision.decline("05", velocity.reason());
        }

        return Decision.approve();
    }

    private record Decision(boolean approved, String responseCode, String reason) {
        static Decision approve() {
            return new Decision(true, "00", null);
        }
        static Decision decline(String code, String reason) {
            return new Decision(false, code, reason);
        }
    }
}
