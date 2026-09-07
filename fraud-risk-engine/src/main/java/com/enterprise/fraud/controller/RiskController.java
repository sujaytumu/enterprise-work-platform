package com.enterprise.fraud.controller;

import com.enterprise.fraud.model.FlaggedTransaction;
import com.enterprise.fraud.model.ScoreRequest;
import com.enterprise.fraud.model.ScoreResponse;
import com.enterprise.fraud.repository.FlaggedTransactionRepository;
import com.enterprise.fraud.service.FlaggingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fraud")
public class RiskController {

    private final FlaggingService flaggingService;
    private final FlaggedTransactionRepository repository;

    public RiskController(FlaggingService flaggingService, FlaggedTransactionRepository repository) {
        this.flaggingService = flaggingService;
        this.repository = repository;
    }

    /** Synchronous scoring call — meant to be invoked before authorization completes. */
    @PostMapping("/score")
    public ResponseEntity<ScoreResponse> score(@Valid @RequestBody ScoreRequest request) {
        return ResponseEntity.ok(flaggingService.scoreAndMaybeFlag(request));
    }

    @GetMapping("/flagged")
    public ResponseEntity<List<FlaggedTransaction>> listFlagged(
            @RequestParam(defaultValue = "PENDING") FlaggedTransaction.ReviewStatus status) {
        return ResponseEntity.ok(repository.findByReviewStatus(status));
    }

    public record ReviewUpdateRequest(FlaggedTransaction.ReviewStatus reviewStatus) {}

    @PatchMapping("/flagged/{id}")
    public ResponseEntity<FlaggedTransaction> updateReview(
            @PathVariable String id, @RequestBody ReviewUpdateRequest update) {
        return repository.findById(id)
                .map(flagged -> {
                    flagged.setReviewStatus(update.reviewStatus());
                    return ResponseEntity.ok(repository.save(flagged));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
