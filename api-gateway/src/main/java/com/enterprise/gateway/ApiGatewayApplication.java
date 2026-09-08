package com.enterprise.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single entry point for banks, merchants, and mobile apps calling into the
 * platform. Responsibilities modeled here: routing to backend services and
 * validating OAuth2 bearer tokens (client-credentials flow, as is standard
 * for bank/merchant server-to-server calls). NOT modeled here: mTLS
 * termination and rate limiting — see docs/ARCHITECTURE.md for what those
 * need in a real deployment (a proper mTLS-terminating ingress/load
 * balancer and a distributed rate limiter backed by Redis, rather than
 * anything embeddable in a demo).
 */
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
