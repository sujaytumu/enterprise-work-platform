package com.enterprise.switchsvc.kafka;

import com.enterprise.switchsvc.iso8583.Iso8583Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventProducer.class);
    private static final String ROUTED_TOPIC = "transactions.routed";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public TransactionEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishRouted(Iso8583Message message) {
        String payload = String.format(
                "{\"stan\":\"%s\",\"merchantId\":\"%s\",\"amount\":%s,\"mti\":\"%s\"}",
                message.getStan(), message.getMerchantId(), message.getAmount(), message.getMti().getCode());

        kafkaTemplate.send(ROUTED_TOPIC, message.getStan(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish routed event for STAN {}", message.getStan(), ex);
                    }
                });
    }
}
