package com.enterprise.switchsvc.routing;

import com.enterprise.switchsvc.iso8583.Iso8583Message;
import com.enterprise.switchsvc.kafka.TransactionEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Routes an incoming ISO 8583-style authorization request to the issuer's
 * core processing engine. In a full multi-issuer network this service would
 * look up the issuer by BIN (the first 6-8 digits of the card number) and
 * route to the correct issuer endpoint; here there's a single issuer
 * (core-processing-engine), which is the realistic starting shape for a
 * single-issuer platform before multi-issuer routing is added.
 */
@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

    private final RestTemplate restTemplate;
    private final TransactionEventProducer eventProducer;
    private final FraudRiskClient fraudRiskClient;

    @Value("${issuer.core-processing-engine.url}")
    private String coreProcessingEngineUrl;

    public RoutingService(RestTemplate restTemplate, TransactionEventProducer eventProducer, FraudRiskClient fraudRiskClient) {
        this.restTemplate = restTemplate;
        this.eventProducer = eventProducer;
        this.fraudRiskClient = fraudRiskClient;
    }

    public Iso8583Message route(Iso8583Message request) {
        eventProducer.publishRouted(request);

        FraudRiskClient.Decision fraudDecision = fraudRiskClient.score(request);
        if (fraudDecision == FraudRiskClient.Decision.BLOCK) {
            log.warn("Transaction blocked by fraud engine for STAN {}", request.getStan());
            return Iso8583Message.authorizationResponse(request, "05");
        }
        if (fraudDecision == FraudRiskClient.Decision.UNAVAILABLE) {
            log.error("Fraud engine unavailable for STAN {}; declining safely", request.getStan());
            return Iso8583Message.authorizationResponse(request, "91");
        }
        if (fraudDecision == FraudRiskClient.Decision.REVIEW) {
            log.info("Fraud engine marked STAN {} for review; continuing issuer authorization", request.getStan());
        }

        Map<String, Object> payload = Map.of(
                "cardToken", request.getPan(),
                "merchantId", request.getMerchantId(),
                "amount", request.getAmount()
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    coreProcessingEngineUrl + "/api/v1/authorizations", payload, Map.class);

            String responseCode = response == null ? "96" : (String) response.get("responseCode");
            return Iso8583Message.authorizationResponse(request, responseCode);

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Card token not found at issuer: {}", request.getPan());
            return Iso8583Message.authorizationResponse(request, "14"); // invalid card number
        } catch (Exception e) {
            log.error("Routing to issuer failed for STAN {}", request.getStan(), e);
            return Iso8583Message.authorizationResponse(request, "91"); // issuer unavailable
        }
    }
}
