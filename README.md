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

## Modules (this phase)

| Module | Purpose |
|---|---|
| `core-processing-engine` | Authorizes transactions: balance/limit checks, account status, velocity/fraud rule checks, emits authorization events |
| `payment-switch` | Parses ISO 8583-style messages, routes transactions between acquirer/issuer, publishes to Kafka |
| `fraud-risk-engine` | Scores transactions via weighted rule-based signals (amount anomaly, velocity, merchant risk), flags high-risk transactions for review, publishes fraud alerts, exposes a review workflow |

Planned next phases (not yet built): Card Management System (CMS), Tokenization
Vault, Clearing & Settlement, API Gateway with OAuth2/mTLS.

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
- Kafka broker: localhost:9092
- Postgres: localhost:5432

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

## Deploying

See `deploy/k8s/` for starter Kubernetes manifests. You'll need to:
1. Push container images to a registry (`docker build` + `docker push`) — swap
   the image names in the manifests.
2. Provide real DB/Kafka connection secrets via `kubectl create secret`.
3. Apply: `kubectl apply -f deploy/k8s/`

This is a starting point, not a turnkey production deployment — review it
against your actual infra, security, and compliance requirements first.
