package com.enterprise.switchsvc.controller;

import com.enterprise.switchsvc.iso8583.Iso8583Codec;
import com.enterprise.switchsvc.iso8583.Iso8583Message;
import com.enterprise.switchsvc.routing.RoutingService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/switch")
public class SwitchController {

    private final RoutingService routingService;
    private final Iso8583Codec codec = new Iso8583Codec();

    public SwitchController(RoutingService routingService) {
        this.routingService = routingService;
    }

    /**
     * Accepts a simplified request shape (merchant-facing convenience) and
     * builds the ISO 8583-style message internally.
     */
    public record SwitchRequest(String cardToken, BigDecimal amount, String merchantId) {}

    @PostMapping("/authorize")
    public ResponseEntity<ObjectNode> authorize(@RequestBody SwitchRequest request) {
        String stan = UUID.randomUUID().toString().substring(0, 8);
        Iso8583Message iso = Iso8583Message.authorizationRequest(
                request.cardToken(), request.amount(), request.merchantId(), stan);

        Iso8583Message response = routingService.route(iso);
        return ResponseEntity.ok(codec.toJson(response));
    }

    /**
     * Accepts a raw ISO 8583-style JSON message directly, for callers that
     * want to work with the message model rather than the convenience shape.
     */
    @PostMapping("/message")
    public ResponseEntity<ObjectNode> handleMessage(@RequestBody ObjectNode rawMessage) {
        Iso8583Message iso = codec.fromJson(rawMessage);
        Iso8583Message response = routingService.route(iso);
        return ResponseEntity.ok(codec.toJson(response));
    }
}
