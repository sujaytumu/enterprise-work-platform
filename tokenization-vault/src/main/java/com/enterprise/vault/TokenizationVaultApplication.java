package com.enterprise.vault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Tokenization Vault is deliberately the ONLY service in this platform
 * that ever stores or handles a raw PAN. Every other service (core engine,
 * switch, fraud engine, CMS) works exclusively with vault-issued tokens.
 * This isolation is the core PCI-DSS design pattern: it shrinks the "cardholder
 * data environment" down to one auditable, tightly-locked-down service instead
 * of spreading raw card numbers across the whole platform.
 */
@SpringBootApplication
public class TokenizationVaultApplication {
    public static void main(String[] args) {
        SpringApplication.run(TokenizationVaultApplication.class, args);
    }
}
