package com.enterprise.vault.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Stores the mapping between a vault-issued token and the encrypted PAN.
 *
 * IMPORTANT: `panCiphertext` here is only demo-grade AES encryption done in
 * application code (see EncryptionService). A real PCI-DSS vault uses a
 * Hardware Security Module (HSM) or a managed KMS (AWS KMS, GCP Cloud HSM,
 * Thales, etc.) so encryption keys never exist in application memory or
 * source code, plus key rotation, split-knowledge/dual-control key
 * ceremonies, and extensive access auditing. None of that is replicated
 * here — this class demonstrates the *shape* of token/PAN separation only.
 */
@Entity
@Table(name = "token_mappings")
public class TokenMapping {

    @Id
    private String token;

    @Column(nullable = false)
    private String panCiphertext;

    /** Last 4 digits only — safe to expose to other services for display purposes. */
    @Column(nullable = false, length = 4)
    private String lastFour;

    /** First 6-8 digits (BIN) — safe to expose for issuer/network routing decisions. */
    @Column(nullable = false, length = 8)
    private String bin;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected TokenMapping() {
        // JPA
    }

    public TokenMapping(String token, String panCiphertext, String lastFour, String bin) {
        this.token = token;
        this.panCiphertext = panCiphertext;
        this.lastFour = lastFour;
        this.bin = bin;
    }

    public String getToken() { return token; }
    public String getPanCiphertext() { return panCiphertext; }
    public String getLastFour() { return lastFour; }
    public String getBin() { return bin; }
    public Instant getCreatedAt() { return createdAt; }
}
