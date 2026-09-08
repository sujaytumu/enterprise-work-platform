package com.enterprise.vault.service;

import com.enterprise.vault.repository.TokenMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@AutoConfigureTestDatabase
class VaultServiceTest {

    @Autowired
    private VaultService vaultService;

    @Test
    void tokenizeAndDetokenizeRoundTrips() {
        String pan = "4111111111111111";

        String token = vaultService.tokenize(pan);
        assertNotNull(token);
        assertTrue(token.startsWith("tok_"));

        String recovered = vaultService.detokenize(token);
        assertEquals(pan, recovered);
    }

    @Test
    void metadataExposesOnlyLastFourAndBin() {
        String pan = "4111111111111111";
        String token = vaultService.tokenize(pan);

        var metadata = vaultService.getMetadata(token);
        assertEquals("1111", metadata.getLastFour());
        assertEquals("41111111", metadata.getBin());
        assertNotEquals(pan, metadata.getPanCiphertext());
    }

    @Test
    void rejectsInvalidPan() {
        assertThrows(IllegalArgumentException.class, () -> vaultService.tokenize("not-a-card"));
    }

    @Test
    void unknownTokenThrows() {
        assertThrows(IllegalArgumentException.class, () -> vaultService.detokenize("tok_doesnotexist"));
    }
}
