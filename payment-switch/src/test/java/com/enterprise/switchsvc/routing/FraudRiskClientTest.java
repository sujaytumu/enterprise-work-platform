package com.enterprise.switchsvc.routing;

import com.enterprise.switchsvc.iso8583.Iso8583Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Lightweight regression guard for the fraud-routing component.
 * Full HTTP integration is exercised by the compose-based CI runtime test.
 */
class FraudRiskClientTest {

    @Test
    void decisionEnumContainsAllOperationalStates() {
        assertNotNull(FraudRiskClient.Decision.ALLOW);
        assertNotNull(FraudRiskClient.Decision.REVIEW);
        assertNotNull(FraudRiskClient.Decision.BLOCK);
        assertNotNull(FraudRiskClient.Decision.UNAVAILABLE);
    }
}
