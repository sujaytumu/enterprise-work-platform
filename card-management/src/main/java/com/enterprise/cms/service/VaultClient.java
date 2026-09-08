package com.enterprise.cms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.Map;

@Service
public class VaultClient {

    private final RestTemplate restTemplate;
    private final SecureRandom random = new SecureRandom();

    @Value("${vault.url}")
    private String vaultUrl;

    public VaultClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public record IssuedCardToken(String token, String lastFour) {}

    /**
     * Generates a synthetic PAN (demo/test purposes only — never a real card
     * number) and sends it to the vault to be tokenized. The CMS immediately
     * discards the raw PAN after this call and only ever holds the token.
     */
    public IssuedCardToken issueCardToken(String binPrefix) {
        String syntheticPan = generateSyntheticPan(binPrefix);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                vaultUrl + "/api/v1/vault/tokenize",
                Map.of("pan", syntheticPan),
                Map.class);

        String token = (String) response.get("token");
        String lastFour = (String) response.get("lastFour");
        return new IssuedCardToken(token, lastFour);
    }

    private String generateSyntheticPan(String binPrefix) {
        StringBuilder sb = new StringBuilder(binPrefix);
        while (sb.length() < 16) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
