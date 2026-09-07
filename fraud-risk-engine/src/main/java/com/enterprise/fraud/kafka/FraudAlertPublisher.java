package com.enterprise.fraud.kafka;

import com.enterprise.fraud.model.FlaggedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class FraudAlertPublisher {

    private static final Logger log = LoggerFactory.getLogger(FraudAlertPublisher.class);
    private static final String ALERTS_TOPIC = "fraud.alerts";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public FraudAlertPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAlert(FlaggedTransaction flagged, String recommendation) {
        String payload = String.format(
                "{\"transactionId\":\"%s\",\"accountId\":\"%s\",\"amount\":%s,\"riskScore\":%s,\"recommendation\":\"%s\",\"reason\":\"%s\"}",
                flagged.getTransactionId(), flagged.getAccountId(), flagged.getAmount(),
                flagged.getRiskScore(), recommendation, escapeJson(flagged.getReason()));

        kafkaTemplate.send(ALERTS_TOPIC, flagged.getAccountId(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish fraud alert for transaction {}", flagged.getTransactionId(), ex);
                    } else {
                        log.warn("Fraud alert published: transaction={} score={} recommendation={}",
                                flagged.getTransactionId(), flagged.getRiskScore(), recommendation);
                    }
                });
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }
}
