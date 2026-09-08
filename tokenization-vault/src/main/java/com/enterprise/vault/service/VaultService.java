package com.enterprise.vault.service;

import com.enterprise.vault.model.TokenMapping;
import com.enterprise.vault.repository.TokenMappingRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class VaultService {

    private final TokenMappingRepository repository;
    private final EncryptionService encryptionService;
    private final SecureRandom secureRandom = new SecureRandom();

    public VaultService(TokenMappingRepository repository, EncryptionService encryptionService) {
        this.repository = repository;
        this.encryptionService = encryptionService;
    }

    /**
     * Tokenizes a raw PAN: generates a random opaque token, encrypts the PAN,
     * and stores the mapping. Returns only the token — the PAN never leaves
     * this method in plaintext form again.
     */
    public String tokenize(String pan) {
        validatePan(pan);

        String token = generateToken();
        String ciphertext = encryptionService.encrypt(pan);
        String lastFour = pan.substring(pan.length() - 4);
        String bin = pan.substring(0, Math.min(8, pan.length() - 4));

        repository.save(new TokenMapping(token, ciphertext, lastFour, bin));
        return token;
    }

    /**
     * Resolves a token back to the raw PAN. In a real system this call would
     * be tightly restricted — only specific, audited callers (e.g. a
     * card-present terminal or a regulator-mandated process) should ever be
     * authorized to detokenize; most services should only ever need the
     * token itself, never the PAN.
     */
    public String detokenize(String token) {
        TokenMapping mapping = repository.findById(token)
                .orElseThrow(() -> new IllegalArgumentException("Unknown token: " + token));
        return encryptionService.decrypt(mapping.getPanCiphertext());
    }

    public TokenMapping getMetadata(String token) {
        return repository.findById(token)
                .orElseThrow(() -> new IllegalArgumentException("Unknown token: " + token));
    }

    private String generateToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return "tok_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void validatePan(String pan) {
        if (pan == null || !pan.matches("\\d{12,19}")) {
            throw new IllegalArgumentException("PAN must be 12-19 digits");
        }
    }
}
