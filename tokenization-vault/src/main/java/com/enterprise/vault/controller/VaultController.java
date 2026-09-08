package com.enterprise.vault.controller;

import com.enterprise.vault.model.TokenMapping;
import com.enterprise.vault.service.VaultService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vault")
public class VaultController {

    private final VaultService vaultService;

    public VaultController(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    public record TokenizeRequest(String pan) {}
    public record TokenizeResponse(String token, String lastFour, String bin) {}
    public record DetokenizeResponse(String pan) {}

    @PostMapping("/tokenize")
    public ResponseEntity<TokenizeResponse> tokenize(@RequestBody TokenizeRequest request) {
        String token = vaultService.tokenize(request.pan());
        TokenMapping metadata = vaultService.getMetadata(token);
        return ResponseEntity.ok(new TokenizeResponse(token, metadata.getLastFour(), metadata.getBin()));
    }

    /**
     * Restricted operation in a real system — see VaultService.detokenize
     * javadoc. Exposed here unauthenticated only because this is a local
     * reference build with no auth layer yet (see api-gateway module).
     */
    @GetMapping("/detokenize/{token}")
    public ResponseEntity<DetokenizeResponse> detokenize(@PathVariable String token) {
        return ResponseEntity.ok(new DetokenizeResponse(vaultService.detokenize(token)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
