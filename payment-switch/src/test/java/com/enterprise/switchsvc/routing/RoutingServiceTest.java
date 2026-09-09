package com.enterprise.switchsvc.routing;

import com.enterprise.switchsvc.iso8583.Iso8583Message;
import com.enterprise.switchsvc.kafka.TransactionEventProducer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RoutingServiceTest {

    @Test
    void blocksTransactionBeforeIssuerCallWhenFraudBlocks() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        TransactionEventProducer producer = mock(TransactionEventProducer.class);
        FraudRiskClient fraud = mock(FraudRiskClient.class);
        when(fraud.score(any())).thenReturn(FraudRiskClient.Decision.BLOCK);

        RoutingService service = new RoutingService(restTemplate, producer, fraud);
        Iso8583Message request = Iso8583Message.authorizationRequest(
                "tok_demo", new BigDecimal("100.00"), "merchant", "123456");

        Iso8583Message response = service.route(request);

        assertEquals("05", response.getResponseCode());
        verify(fraud).score(request);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void failsClosedWhenFraudServiceUnavailable() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        TransactionEventProducer producer = mock(TransactionEventProducer.class);
        FraudRiskClient fraud = mock(FraudRiskClient.class);
        when(fraud.score(any())).thenReturn(FraudRiskClient.Decision.UNAVAILABLE);

        RoutingService service = new RoutingService(restTemplate, producer, fraud);
        Iso8583Message request = Iso8583Message.authorizationRequest(
                "tok_demo", new BigDecimal("100.00"), "merchant", "123456");

        Iso8583Message response = service.route(request);

        assertEquals("91", response.getResponseCode());
        verifyNoInteractions(restTemplate);
    }
}
