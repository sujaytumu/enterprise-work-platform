package com.enterprise.core.service;

import com.enterprise.core.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventPublisher.class);

    private static final String APPROVED_TOPIC = "transactions.authorized";
    private static final String DECLINED_TOPIC = "transactions.declined";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public TransactionEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(Transaction transaction) {
        String topic = transaction.getStatus() == Transaction.TransactionStatus.APPROVED
                ? APPROVED_TOPIC
                : DECLINED_TOPIC;

        String payload = String.format(
                "{\"transactionId\":\"%s\",\"accountId\":\"%s\",\"merchantId\":\"%s\",\"amount\":%s,\"status\":\"%s\"}",
                transaction.getId(), transaction.getAccountId(), transaction.getMerchantId(),
                transaction.getAmount(), transaction.getStatus());

        kafkaTemplate.send(topic, transaction.getAccountId(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish transaction event {} to {}", transaction.getId(), topic, ex);
                    } else {
                        log.info("Published transaction event {} to {}", transaction.getId(), topic);
                    }
                });
    }
}
