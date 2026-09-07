package com.enterprise.fraud.service;

import com.enterprise.fraud.kafka.FraudAlertPublisher;
import com.enterprise.fraud.model.FlaggedTransaction;
import com.enterprise.fraud.model.ScoreRequest;
import com.enterprise.fraud.model.ScoreResponse;
import com.enterprise.fraud.repository.FlaggedTransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class FlaggingService {

    private final RiskScoringService riskScoringService;
    private final FlaggedTransactionRepository repository;
    private final FraudAlertPublisher alertPublisher;

    public FlaggingService(RiskScoringService riskScoringService,
                            FlaggedTransactionRepository repository,
                            FraudAlertPublisher alertPublisher) {
        this.riskScoringService = riskScoringService;
        this.repository = repository;
        this.alertPublisher = alertPublisher;
    }

    public ScoreResponse scoreAndMaybeFlag(ScoreRequest request) {
        ScoreResponse response = riskScoringService.score(request);

        if (!"ALLOW".equals(response.getRecommendation())) {
            String reason = String.join("; ", response.getTriggeredReasons());
            FlaggedTransaction flagged = new FlaggedTransaction(
                    request.getTransactionId(), request.getAccountId(),
                    request.getAmount(), response.getRiskScore(), reason);
            repository.save(flagged);
            alertPublisher.publishAlert(flagged, response.getRecommendation());
        }

        return response;
    }
}
