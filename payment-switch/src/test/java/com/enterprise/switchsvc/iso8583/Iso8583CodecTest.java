package com.enterprise.switchsvc.iso8583;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Iso8583CodecTest {

    private final Iso8583Codec codec = new Iso8583Codec();

    @Test
    void roundTripsAuthorizationRequest() {
        Iso8583Message original = Iso8583Message.authorizationRequest(
                "tok_abc123", new BigDecimal("42.50"), "merchant_9", "00000001");

        ObjectNode json = codec.toJson(original);
        Iso8583Message parsed = codec.fromJson(json);

        assertEquals(Iso8583Message.MTI.AUTHORIZATION_REQUEST, parsed.getMti());
        assertEquals("tok_abc123", parsed.getPan());
        assertEquals(0, new BigDecimal("42.50").compareTo(parsed.getAmount()));
        assertEquals("merchant_9", parsed.getMerchantId());
        assertEquals("00000001", parsed.getStan());
    }

    @Test
    void roundTripsAuthorizationResponse() {
        Iso8583Message request = Iso8583Message.authorizationRequest(
                "tok_abc123", new BigDecimal("10.00"), "merchant_1", "00000002");
        Iso8583Message response = Iso8583Message.authorizationResponse(request, "00");

        ObjectNode json = codec.toJson(response);
        Iso8583Message parsed = codec.fromJson(json);

        assertEquals(Iso8583Message.MTI.AUTHORIZATION_RESPONSE, parsed.getMti());
        assertEquals("00", parsed.getResponseCode());
    }
}
