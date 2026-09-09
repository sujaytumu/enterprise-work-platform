#!/usr/bin/env bash
set -euo pipefail

services=(
  "api-gateway:8080"
  "core-processing-engine:8081"
  "payment-switch:8082"
  "fraud-risk-engine:8083"
  "tokenization-vault:8084"
  "card-management:8085"
  "clearing-settlement:8086"
)

for service in "${services[@]}"; do
  name="${service%%:*}"
  port="${service##*:}"
  url="http://localhost:${port}/actuator/health"
  echo "Checking ${name}: ${url}"

  for ((i=1; i<=120; i++)); do
    if curl -fsS "$url" >/dev/null; then
      echo "OK: ${name}"
      break
    fi
    if [[ "$i" == "120" ]]; then
      echo "FAILED: ${name} did not become healthy"
      exit 1
    fi
    sleep 2
  done
done

curl -fsS http://localhost:3000/ >/dev/null
echo "OK: client"

echo "OK: API gateway is healthy; downstream services were verified directly via actuator."
echo "Smoke test passed."
