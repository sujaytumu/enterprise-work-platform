#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
DIRECT_E2E="${DIRECT_E2E:-false}"

# The gateway is intentionally secured outside the local demo profile. CI can
# exercise the business flow directly while gateway security is tested separately.
if [[ "$DIRECT_E2E" == "true" ]]; then
  CARD_URL="${CARD_URL:-http://localhost:8085}"
  VAULT_URL="${VAULT_URL:-http://localhost:8084}"
  CORE_URL="${CORE_URL:-http://localhost:8081}"
  SWITCH_URL="${SWITCH_URL:-http://localhost:8082}"
else
  CARD_URL="$BASE_URL/cards"
  VAULT_URL="$BASE_URL/vault"
  CORE_URL="$BASE_URL/core"
  SWITCH_URL="$BASE_URL/switch"
fi
CUSTOMER_ID="demo-customer-$(date +%s)"

echo "1/6 Issuing demo card..."
CARD=$(curl -fsS -X POST "$CARD_URL/api/v1/cards" \
  -H 'Content-Type: application/json' \
  -d "{\"customerId\":\"$CUSTOMER_ID\",\"cardType\":\"VIRTUAL\",\"binPrefix\":\"411111\"}")
CARD_ID=$(echo "$CARD" | python3 -c 'import sys,json; print(json.load(sys.stdin)["id"])')
echo "Card: $CARD_ID"

echo "2/6 Activating card..."
curl -fsS -X POST "$CARD_URL/api/v1/cards/$CARD_ID/activate" >/dev/null

echo "3/6 Tokenizing synthetic PAN..."
TOKEN=$(curl -fsS -X POST "$VAULT_URL/api/v1/vault/tokenize" \
  -H 'Content-Type: application/json' \
  -d '{"pan":"4111111111111111"}' | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')
echo "Token created"

echo "4/6 Creating demo account..."
curl -fsS -X POST "$CORE_URL/api/v1/accounts" \
  -H 'Content-Type: application/json' \
  -d "{\"cardToken\":\"$TOKEN\",\"balance\":1000.00,\"dailyLimit\":500.00}" >/dev/null

echo "5/6 Authorizing payment..."
RESPONSE=$(curl -fsS -X POST "$SWITCH_URL/api/v1/switch/authorize" \
  -H 'Content-Type: application/json' \
  -d "{\"cardToken\":\"$TOKEN\",\"amount\":25.00,\"merchantId\":\"DEMO_MERCHANT\"}")
echo "$RESPONSE"
RESPONSE_CODE=$(echo "$RESPONSE" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("39") or d.get("responseCode") or "")')
if [[ "$RESPONSE_CODE" != "00" ]]; then
  echo "E2E authorization was declined with response code: $RESPONSE_CODE" >&2
  exit 1
fi

echo "6/6 E2E demo completed successfully."
