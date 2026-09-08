#!/usr/bin/env sh
set -eu

services="api-gateway:8080 core-processing-engine:8081 payment-switch:8082 fraud-risk-engine:8083 tokenization-vault:8084 card-management:8085 clearing-settlement:8086"

for service in $services; do
  name="${service%%:*}"
  port="${service##*:}"
  url="http://localhost:${port}/actuator/health"
  echo "Checking ${name}: ${url}"
  i=0
  until curl -fsS "$url" >/dev/null; do
    i=$((i + 1))
    if [ "$i" -ge 30 ]; then
      echo "FAILED: ${name} did not become healthy"
      exit 1
    fi
    sleep 2
  done
  echo "OK: ${name}"
done

curl -fsS http://localhost:3000/ >/dev/null
echo "OK: client"
echo "Smoke test passed."
