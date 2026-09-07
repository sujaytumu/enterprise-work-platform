package com.enterprise.fraud.service;

import com.enterprise.fraud.model.ScoreRequest;
import com.enterprise.fraud.model.ScoreResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Scores a transaction for fraud risk.
 *
 * This is a transparent, weighted rule-based scorer — NOT a trained ML
 * model. A real fraud engine at this scale would use a model (gradient
 * boosted trees or a neural net) trained on millions of labeled historical
 * transactions, continuously retrained, with features like device
 * fingerprinting, IP geolocation, card-present vs card-not-present, and
 * behavioral biometrics. Building and validating that model is a data
 * science project in its own right, not something to fabricate here.
 *
 * What this class demonstrates instead: the *shape* of a production risk
 * engine — multiple independent signals, each contributing a weighted
 * score, combined into a single risk value with an explainable list of
 * triggered reasons (real fraud systems need explainability for compliance
 * and manual review, not just a black-box number).
 */
@Service
public class RiskScoringService {

    @Value("${fraud.thresholds.block:0.75}")
    private double blockThreshold;

    @Value("${fraud.thresholds.review:0.4}")
    private double reviewThreshold;

    public ScoreResponse score(ScoreRequest request) {
        List<String> reasons = new ArrayList<>();
        double score = 0.0;

        // Signal 1: amount is a large multiple of the account's typical spend.
        if (request.getAccountAvgAmount() != null &&
                request.getAccountAvgAmount().compareTo(BigDecimal.ZERO) > 0) {
            double ratio = request.getAmount().doubleValue() / request.getAccountAvgAmount().doubleValue();
            if (ratio > 10) {
                score += 0.35;
                reasons.add(String.format("Amount is %.1fx the account's average transaction", ratio));
            } else if (ratio > 5) {
                score += 0.15;
                reasons.add(String.format("Amount is %.1fx the account's average transaction", ratio));
            }
        }

        // Signal 2: high transaction frequency in a short window (card testing pattern).
        if (request.getTransactionsLastHour() != null) {
            if (request.getTransactionsLastHour() >= 10) {
                score += 0.35;
                reasons.add(request.getTransactionsLastHour() + " transactions in the last hour");
            } else if (request.getTransactionsLastHour() >= 5) {
                score += 0.15;
                reasons.add(request.getTransactionsLastHour() + " transactions in the last hour");
            }
        }

        // Signal 3: merchant category commonly associated with fraud cash-out.
        if (request.isHighRiskMerchantCategory()) {
            score += 0.2;
            reasons.add("Merchant category is high-risk (e.g. gift cards, crypto, wire transfer)");
        }

        // Signal 4: round, large amounts are statistically more common in testing/fraud than organic spend.
        if (isRoundAmount(request.getAmount()) && request.getAmount().doubleValue() >= 200) {
            score += 0.1;
            reasons.add("Large round-number amount");
        }

        score = Math.min(score, 1.0);

        String recommendation;
        if (score >= blockThreshold) {
            recommendation = "BLOCK";
        } else if (score >= reviewThreshold) {
            recommendation = "REVIEW";
        } else {
            recommendation = "ALLOW";
        }

        return new ScoreResponse(request.getTransactionId(), round(score), recommendation, reasons);
    }

    private boolean isRoundAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().scale() <= 0 && amount.remainder(BigDecimal.valueOf(50)).signum() == 0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
