#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
CUSTOMER_ID="demo-customer-$(date +%s)"

echo "1/6 Issuing demo card..."
CARD=$(curl -fsS -X POST "$BASE_URL/cards/api/v1/cards" \
  -H 'Content-Type: application/json' \
  -d "{\"customerId\":\"$CUSTOMER_ID\",\"cardType\":\"DEBIT\",\"binPrefix\":\"411111\"}")
CARD_ID=$(echo "$CARD" | python3 -c 'import sys,json; print(json.load(sys.stdin)["id"])')
echo "Card: $CARD_ID"

echo "2/6 Activating card..."
curl -fsS -X POST "$BASE_URL/cards/api/v1/cards/$CARD_ID/activate" >/dev/null

echo "3/6 Tokenizing synthetic PAN..."
TOKEN=$(curl -fsS -X POST "$BASE_URL/vault/api/v1/vault/tokenize" \
  -H 'Content-Type: application/json' \
  -d '{"pan":"4111111111111111"}' | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')
echo "Token created"

echo "4/6 Creating demo account..."
curl -fsS -X POST "$BASE_URL/core/api/v1/accounts" \
  -H 'Content-Type: application/json' \
  -d "{\"cardToken\":\"$TOKEN\",\"balance\":1000.00,\"dailyLimit\":500.00}" >/dev/null

echo "5/6 Authorizing payment..."
RESPONSE=$(curl -fsS -X POST "$BASE_URL/switch/api/v1/switch/authorize" \
  -H 'Content-Type: application/json' \
  -d "{\"cardToken\":\"$TOKEN\",\"amount\":25.00,\"merchantId\":\"DEMO_MERCHANT\"}")
echo "$RESPONSE"

echo "6/6 E2E demo completed successfully."
