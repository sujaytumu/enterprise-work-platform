package com.enterprise.fraud.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ScoreRequest {

    @NotBlank
    private String transactionId;

    @NotBlank
    private String accountId;

    @NotBlank
    private String merchantId;

    @NotNull
    private BigDecimal amount;

    /** Average of the account's recent transaction amounts, for anomaly comparison. */
    private BigDecimal accountAvgAmount;

    /** How many transactions this account has made in the last hour. */
    private Integer transactionsLastHour;

    /** Whether the merchant category is one commonly associated with fraud testing (e.g. gift cards, crypto). */
    private boolean highRiskMerchantCategory;

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getAccountAvgAmount() { return accountAvgAmount; }
    public void setAccountAvgAmount(BigDecimal accountAvgAmount) { this.accountAvgAmount = accountAvgAmount; }
    public Integer getTransactionsLastHour() { return transactionsLastHour; }
    public void setTransactionsLastHour(Integer transactionsLastHour) { this.transactionsLastHour = transactionsLastHour; }
    public boolean isHighRiskMerchantCategory() { return highRiskMerchantCategory; }
    public void setHighRiskMerchantCategory(boolean highRiskMerchantCategory) { this.highRiskMerchantCategory = highRiskMerchantCategory; }
}
