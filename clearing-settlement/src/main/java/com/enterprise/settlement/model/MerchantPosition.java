package com.enterprise.settlement.model;

import java.math.BigDecimal;

/** Net amount owed to (or from) a merchant for a settlement batch. */
public record MerchantPosition(String merchantId, BigDecimal netAmount, int transactionCount) {
}
