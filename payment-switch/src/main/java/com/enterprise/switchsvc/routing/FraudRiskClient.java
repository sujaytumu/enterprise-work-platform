package com.enterprise.switchsvc.routing;

import com.enterprise.switchsvc.iso8583.Iso8583Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Synchronous fraud decision boundary used before issuer authorization.
 * This demo fails closed when the fraud service is unavailable: a payment
 * cannot be approved when a required risk decision cannot be obtained.
 */
@Component
public class FraudRiskClient {

    private static final Logger log = LoggerFactory.getLogger(FraudRiskClient.class);

    private final RestTemplate restTemplate;

    @Value("${fraud-risk-engine.url}")
    private String fraudRiskEngineUrl;

    public FraudRiskClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Decision score(Iso8583Message request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transactionId", request.getStan());
        payload.put("accountId", request.getPan());
        payload.put("merchantId", request.getMerchantId());
        payload.put("amount", request.getAmount());
        payload.put("accountAvgAmount", request.getAmount());
        payload.put("transactionsLastHour", 0);
        payload.put("highRiskMerchantCategory", false);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    fraudRiskEngineUrl + "/api/v1/fraud/score", payload, Map.class);

            if (response == null || response.get("recommendation") == null) {
                log.warn("Fraud service returned an invalid response for STAN {}", request.getStan());
                return Decision.UNAVAILABLE;
            }

            String recommendation = String.valueOf(response.get("recommendation"));
            return switch (recommendation) {
                case "ALLOW" -> Decision.ALLOW;
                case "REVIEW" -> Decision.REVIEW;
                case "BLOCK" -> Decision.BLOCK;
                default -> Decision.UNAVAILABLE;
            };
        } catch (Exception e) {
            log.error("Fraud scoring failed for STAN {}", request.getStan(), e);
            return Decision.UNAVAILABLE;
        }
    }

    public enum Decision {
        ALLOW, REVIEW, BLOCK, UNAVAILABLE
    }
}
