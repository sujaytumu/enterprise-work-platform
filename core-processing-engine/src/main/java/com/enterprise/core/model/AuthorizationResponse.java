package com.enterprise.core.model;

public class AuthorizationResponse {

    private String transactionId;
    private String status; // APPROVED / DECLINED
    private String responseCode; // ISO-8583-style: 00 approved, 05 do not honor, 51 insufficient funds, 61 exceeds limit, 62 restricted
    private String message;

    public AuthorizationResponse(String transactionId, String status, String responseCode, String message) {
        this.transactionId = transactionId;
        this.status = status;
        this.responseCode = responseCode;
        this.message = message;
    }

    public String getTransactionId() { return transactionId; }
    public String getStatus() { return status; }
    public String getResponseCode() { return responseCode; }
    public String getMessage() { return message; }
}
