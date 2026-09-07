package com.enterprise.fraud.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Passively monitors the transaction event stream published by the core
 * processing engine. This is the "always watching" side of a fraud engine —
 * separate from the synchronous /score endpoint that a caller can invoke
 * before approving a transaction. In a full build, patterns spotted here
 * (e.g. a burst of declines across many accounts from the same merchant,
 * suggesting a compromised terminal) would feed back into blocklists or
 * trigger analyst review, rather than just being logged.
 */
@Service
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    @KafkaListener(topics = "transactions.authorized", groupId = "fraud-risk-engine")
    public void onAuthorized(String payload) {
        log.info("Observed authorized transaction: {}", payload);
        // Placeholder for stream-level pattern detection (e.g. spike detection
        // across accounts/merchants) — out of scope for this reference build.
    }

    @KafkaListener(topics = "transactions.declined", groupId = "fraud-risk-engine")
    public void onDeclined(String payload) {
        log.info("Observed declined transaction: {}", payload);
    }
}
