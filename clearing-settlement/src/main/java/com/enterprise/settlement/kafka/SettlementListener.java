package com.enterprise.settlement.kafka;

import com.enterprise.settlement.model.SettledTransaction;
import com.enterprise.settlement.repository.SettledTransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Consumes the core engine's `transactions.authorized` stream and records
 * each transaction into the settlement ledger, ready to be picked up by
 * the next end-of-day batch run. Declined transactions are intentionally
 * NOT consumed here — nothing to settle if the transaction never happened.
 */
@Service
public class SettlementListener {

    private static final Logger log = LoggerFactory.getLogger(SettlementListener.class);
    private final SettledTransactionRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public SettlementListener(SettledTransactionRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "transactions.authorized", groupId = "clearing-settlement")
    public void onAuthorized(String payload) {
        try {
            JsonNode node = mapper.readTree(payload);
            SettledTransaction record = new SettledTransaction(
                    node.get("transactionId").asText(),
                    node.get("accountId").asText(),
                    node.get("merchantId").asText(),
                    new BigDecimal(node.get("amount").asText()));
            repository.save(record);
            log.info("Recorded authorized transaction {} for settlement", node.get("transactionId").asText());
        } catch (DataIntegrityViolationException duplicate) {
            log.info("Ignoring duplicate settlement event: {}", payload);
        } catch (Exception e) {
            log.error("Failed to record transaction for settlement: {}", payload, e);
        }
    }
}
