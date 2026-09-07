#!/usr/bin/env bash
# Quick end-to-end smoke test against `docker compose up`.
# Creates a demo account, then sends a transaction through the switch.
set -euo pipefail

CORE_URL="http://localhost:8081"
SWITCH_URL="http://localhost:8082"

echo "1) Creating a demo account with a $200 balance and $500 daily limit..."
curl -s -X POST "$CORE_URL/api/v1/accounts" \
  -H "Content-Type: application/json" \
  -d '{"cardToken":"tok_demo_001","balance":200.00,"dailyLimit":500.00}' | tee /tmp/account.json
echo

echo "2) Sending a \$25 transaction through the payment switch..."
curl -s -X POST "$SWITCH_URL/api/v1/switch/authorize" \
  -H "Content-Type: application/json" \
  -d '{"cardToken":"tok_demo_001","amount":25.00,"merchantId":"merchant_coffee_shop"}'
echo
echo

echo "3) Sending a transaction that exceeds the balance (should decline, code 51)..."
curl -s -X POST "$SWITCH_URL/api/v1/switch/authorize" \
  -H "Content-Type: application/json" \
  -d '{"cardToken":"tok_demo_001","amount":9999.00,"merchantId":"merchant_coffee_shop"}'
echo
echo

FRAUD_URL="http://localhost:8083"
echo "4) Scoring a suspicious transaction directly against the fraud engine..."
echo "   (large amount vs. average, high transaction velocity, high-risk merchant)"
curl -s -X POST "$FRAUD_URL/api/v1/fraud/score" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId":"txn_demo_001",
    "accountId":"acct_demo_001",
    "merchantId":"merchant_giftcards",
    "amount":500.00,
    "accountAvgAmount":20.00,
    "transactionsLastHour":12,
    "highRiskMerchantCategory":true
  }'
echo
echo

echo "5) Listing flagged transactions pending review..."
curl -s "$FRAUD_URL/api/v1/fraud/flagged?status=PENDING"
echo
