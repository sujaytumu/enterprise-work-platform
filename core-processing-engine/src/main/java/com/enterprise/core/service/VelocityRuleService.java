package com.enterprise.core.service;

import com.enterprise.core.model.Transaction;
import com.enterprise.core.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Illustrative rule-based fraud check: flags an account that has too many
 * approved transactions in a short window. In a full build this logic would
 * live in a dedicated Fraud & Risk service backed by a trained model —
 * see docs/ARCHITECTURE.md. This is intentionally simple and in-process so
 * the authorization flow is complete and demonstrable end to end.
 */
@Service
public class VelocityRuleService {

    private final TransactionRepository transactionRepository;

    @Value("${fraud.velocity.window-seconds:60}")
    private long windowSeconds;

    @Value("${fraud.velocity.max-transactions:5}")
    private int maxTransactions;

    public VelocityRuleService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public VelocityCheckResult check(String accountId) {
        Instant since = Instant.now().minus(windowSeconds, ChronoUnit.SECONDS);
        List<Transaction> recent = transactionRepository.findByAccountIdAndCreatedAtAfter(accountId, since);

        long approvedRecently = recent.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.APPROVED)
                .count();

        if (approvedRecently >= maxTransactions) {
            return VelocityCheckResult.flagged(
                    "Velocity rule triggered: " + approvedRecently +
                    " approved transactions in the last " + windowSeconds + "s");
        }
        return VelocityCheckResult.clear();
    }

    public record VelocityCheckResult(boolean flagged, String reason) {
        static VelocityCheckResult flagged(String reason) {
            return new VelocityCheckResult(true, reason);
        }
        static VelocityCheckResult clear() {
            return new VelocityCheckResult(false, null);
        }
    }
}
