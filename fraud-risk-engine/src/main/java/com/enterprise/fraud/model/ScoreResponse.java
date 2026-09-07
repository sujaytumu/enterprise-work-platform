package com.enterprise.fraud.model;

import java.util.List;

public class ScoreResponse {

    private String transactionId;
    private double riskScore; // 0.0 (low risk) to 1.0 (high risk)
    private String recommendation; // ALLOW, REVIEW, BLOCK
    private List<String> triggeredReasons;

    public ScoreResponse(String transactionId, double riskScore, String recommendation, List<String> triggeredReasons) {
        this.transactionId = transactionId;
        this.riskScore = riskScore;
        this.recommendation = recommendation;
        this.triggeredReasons = triggeredReasons;
    }

    public String getTransactionId() { return transactionId; }
    public double getRiskScore() { return riskScore; }
    public String getRecommendation() { return recommendation; }
    public List<String> getTriggeredReasons() { return triggeredReasons; }
}
