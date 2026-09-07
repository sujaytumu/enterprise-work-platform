package com.enterprise.fraud.service;

import com.enterprise.fraud.model.ScoreRequest;
import com.enterprise.fraud.model.ScoreResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RiskScoringServiceTest {

    private RiskScoringService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new RiskScoringService();
        setField("blockThreshold", 0.75);
        setField("reviewThreshold", 0.4);
    }

    private void setField(String name, double value) throws Exception {
        Field field = RiskScoringService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }

    private ScoreRequest baseRequest() {
        ScoreRequest request = new ScoreRequest();
        request.setTransactionId("txn_1");
        request.setAccountId("acct_1");
        request.setMerchantId("merchant_1");
        request.setAmount(new BigDecimal("20.00"));
        return request;
    }

    @Test
    void normalTransactionIsAllowed() {
        ScoreRequest request = baseRequest();
        request.setAccountAvgAmount(new BigDecimal("18.00"));
        request.setTransactionsLastHour(1);

        ScoreResponse response = service.score(request);

        assertEquals("ALLOW", response.getRecommendation());
        assertTrue(response.getTriggeredReasons().isEmpty());
    }

    @Test
    void largeAmountSpikeTriggersReview() {
        ScoreRequest request = baseRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setAccountAvgAmount(new BigDecimal("20.00")); // 25x average

        ScoreResponse response = service.score(request);

        assertNotEquals("ALLOW", response.getRecommendation());
        assertFalse(response.getTriggeredReasons().isEmpty());
    }

    @Test
    void cardTestingPatternPlusHighRiskMerchantTriggersBlock() {
        ScoreRequest request = baseRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setAccountAvgAmount(new BigDecimal("20.00")); // +0.35
        request.setTransactionsLastHour(12); // +0.35
        request.setHighRiskMerchantCategory(true); // +0.2

        ScoreResponse response = service.score(request);

        assertEquals("BLOCK", response.getRecommendation());
        assertTrue(response.getRiskScore() >= 0.75);
        assertEquals(3, response.getTriggeredReasons().size());
    }

    @Test
    void scoreIsCappedAtOne() {
        ScoreRequest request = baseRequest();
        request.setAmount(new BigDecimal("1000.00"));
        request.setAccountAvgAmount(new BigDecimal("1.00"));
        request.setTransactionsLastHour(20);
        request.setHighRiskMerchantCategory(true);

        ScoreResponse response = service.score(request);

        assertTrue(response.getRiskScore() <= 1.0);
    }
}
