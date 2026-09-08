# Enterprise Payment Platform (Reference Implementation)

A demo/reference implementation of a bank-grade card issuance & payment processing
platform, inspired by systems like Visa's network. Built to show the architecture
and core logic of such a system end-to-end — **not** a certified, production-ready
payment processor.

## ⚠️ Important disclaimers — read before using this anywhere near real money

- **Not PCI-DSS certified.** PCI-DSS compliance requires a formal audit by a
  Qualified Security Assessor (QSA). No codebase, AI-generated or otherwise,
  is "PCI compliant" on its own.
- **No real card data.** All examples use synthetic/dummy PANs, accounts, and
  transactions. Never point this at real cardholder data.
- **No real bank connectivity.** The ISO 8583 switch and routing logic model
  the protocol and flow, but there's no real acquirer/issuer network connection.
- **Fraud engine is illustrative.** It uses simple rule-based + a placeholder
  ML scoring hook, not a production fraud model.
- Treat this as a learning/prototype scaffold: solid architecture and working
  logic you can build on, reviewed and hardened by security engineers before
  it goes anywhere near production traffic.

## Modules

| Module | Port | Purpose |
|---|---|---|
| `api-gateway` | 8080 | Single entry point; OAuth2 JWT validation; routes to all backend services |
| `core-processing-engine` | 8081 | Authorizes transactions: balance/limit checks, account status, velocity checks |
| `payment-switch` | 8082 | Parses ISO 8583-style messages, routes between acquirer/issuer, publishes to Kafka |
| `fraud-risk-engine` | 8083 | Weighted rule-based fraud scoring, flags high-risk transactions, publishes alerts |
| `tokenization-vault` | 8084 | The only service that ever handles a raw PAN; issues tokens, AES-256-GCM encryption at rest |
| `card-management` | 8085 | Card issuance, activation, PIN set (hashed), lifecycle state machine |
| `clearing-settlement` | 8086 | Consumes authorized transactions, nets positions by merchant, end-of-day batch job |

All seven modules are built, each with Prometheus metrics, Zipkin tracing,
and structured JSON logging wired in (see Observability section below). See
`docs/ARCHITECTURE.md` for how they fit together and what's intentionally
simplified vs. a real production system.

## Stack

- Java 17, Spring Boot 3.x
- Kafka (event streaming: `transactions.authorized`, `transactions.declined`, `transactions.routed`)
- PostgreSQL (via Docker Compose) / H2 for quick local runs
- Docker Compose for local dev; Kubernetes manifests in `deploy/k8s` for real clusters

## Running locally

```bash
docker compose up --build
```

- Core Processing Engine: http://localhost:8081
- Payment Switch: http://localhost:8082
- Fraud & Risk Engine: http://localhost:8083
- Tokenization Vault: http://localhost:8084
- Card Management: http://localhost:8085
- Clearing & Settlement: http://localhost:8086
- API Gateway (single entry point, routes to everything above): http://localhost:8080
- Kafka broker: localhost:9092
- Postgres: localhost:5432

The gateway runs with security disabled by default in Docker Compose (`nosecurity`
profile) purely so you can exercise the whole flow locally without standing up
an OAuth2 provider. **Remove that profile before deploying anywhere reachable
outside your machine** — see `api-gateway/src/main/resources/application.yml`.

## Observability

All 7 services ship with metrics, distributed tracing, and structured logs
built in. Run the full stack with:

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml up --build
```

Then open Grafana at http://localhost:3000 (`admin`/`admin`) for a
pre-built dashboard, or Zipkin at http://localhost:9411 to trace a single
request across every service it touched. Full details in
`docs/OBSERVABILITY.md`.

## Pushing this to your GitHub repo

This zip is not connected to git yet. From the extracted folder:

```bash
cd enterprise-work-platform
git init
git remote add origin https://github.com/sujaytumu/enterprise-work-platform.git
git add .
git commit -m "Initial scaffold: core processing engine + payment switch"
git branch -M main
git push -u origin main
```

(Use a freshly generated token or SSH key — never reuse a token that was ever
pasted in a chat or logged anywhere.)

## Deploying to a real cluster

See `deploy/k8s/` for manifests covering all seven services. Steps:

1. Build and push each image to a registry you control:
   ```bash
   docker build -t ghcr.io/YOUR_USERNAME/core-processing-engine:latest ./core-processing-engine
   docker push ghcr.io/YOUR_USERNAME/core-processing-engine:latest
   # ...repeat for each of the 7 services
   ```
2. Update the `image:` field in each `deploy/k8s/*.yaml` to point at your pushed images.
3. Create the real secrets (never commit these):
   ```bash
   kubectl create secret generic payments-db-secret \
     --from-literal=DB_USER=payments \
     --from-literal=DB_PASSWORD='<a real generated password>' \
     --from-literal=VAULT_ENCRYPTION_KEY="$(openssl rand -base64 32)"
   ```
4. Set a real OAuth2 issuer in `deploy/k8s/config.yaml`'s `OAUTH2_ISSUER_URI`
   (Keycloak, Auth0, Okta, AWS Cognito, etc. — none is included here).
5. Apply everything: `kubectl apply -f deploy/k8s/`
6. Point a real Kafka cluster and Postgres instance at the connection details
   in the ConfigMap — this repo's Docker Compose Kafka/Postgres are for local
   dev only, not durable enough for production.

### Before this touches real money or real cardholder data

This is a **deployment-ready reference implementation** in the sense that
the manifests, health checks, and service wiring are complete and will run —
not in the sense that it's ready for production payment traffic. Close these
gaps first:

- [ ] Formal PCI-DSS audit by a Qualified Security Assessor
- [ ] Replace the vault's application-level AES key with a real KMS/HSM
      (AWS KMS, GCP Cloud HSM, Thales, etc.)
- [ ] Stand up a real OAuth2/OIDC provider and remove the `nosecurity` profile
      everywhere
- [ ] Add mTLS termination at the ingress (not included — see ARCHITECTURE.md)
- [ ] Replace the fraud engine's rule-based scorer with a model trained on
      real labeled transaction data, validated by your risk team
- [ ] Load testing against your actual expected transaction volume
- [ ] Production alerting (Prometheus Alertmanager / Grafana alert rules) and
      a sane trace sampling rate — this repo samples 100% of traces and has
      no alerts configured, both fine for a demo, wrong for real volume
- [ ] Real card network connectivity (this platform doesn't connect to Visa/
      Mastercard/etc. — it models the pattern, not the network membership)
- [ ] Security review of every service by people who aren't the AI that wrote it
