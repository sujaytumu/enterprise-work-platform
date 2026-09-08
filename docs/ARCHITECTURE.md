# Architecture Overview

## Flow (current phase)

```
Merchant/Acquirer
      │  ISO 8583-style auth request (JSON-modeled)
      ▼
┌─────────────────┐      routes to issuer       ┌──────────────────────┐
│  Payment Switch   │ ───────────────────────────▶│ Core Processing Engine │
│  (port 8082)       │                             │  (port 8081)            │
│  - parses message   │◀───────────────────────────│  - balance/limit check   │
│  - routing decision │   approve/decline response  │  - account status check  │
│  - publishes event  │                             │  - velocity/fraud rules  │
└─────────────────┘                             │  - publishes event      │
                                                  └──────────────────────┘
                                                            │
                                                            ▼
                                                   Kafka topics:
                                                   - transactions.authorized
                                                   - transactions.declined
                                                   - transactions.routed
```

## Why these pieces

- **Payment Switch as its own service**: mirrors how real networks separate
  "routing/messaging" from "authorization logic" — acquirers and issuers are
  usually different institutions, so the switch is the neutral middle party.
- **ISO 8583 modeled, not full spec**: the real spec has hundreds of fields
  and bitmap-based encoding. We model a practical subset (PAN, amount, MTI,
  merchant, response code) as a typed Java object with a from/to-map parser,
  enough to demonstrate the pattern without months of spec work.
- **Kafka for events, not for the authorization path itself**: authorization
  must be synchronous and low-latency (real systems target <300ms end-to-end).
  Kafka is used for what it's good at — fan-out to fraud monitoring,
  analytics, and settlement, after the synchronous decision is made.
- **Velocity/fraud checks live in the core engine for now**: in a full build
  this would call out to a dedicated Fraud & Risk service; here it's a
  simple in-process rule set so the authorization flow is complete and
  demonstrable without standing up an ML service yet.

## Data model (current phase)

- `Account`: id, cardNumber (masked/tokenized in real system), balance,
  dailyLimit, status (ACTIVE/BLOCKED/CLOSED)
- `Transaction`: id, accountId, amount, merchantId, status, timestamp, mti

## Fraud & Risk Engine

`fraud-risk-engine` (port 8083) adds a `/api/v1/fraud/score` endpoint that
scores a transaction using multiple weighted signals — amount vs. account
average, transaction velocity in the last hour, high-risk merchant category,
and round-number amounts — and returns `ALLOW` / `REVIEW` / `BLOCK` with an
explainable list of triggered reasons. Anything above the review threshold
is persisted as a `FlaggedTransaction` and published to a `fraud.alerts`
Kafka topic. A separate Kafka consumer passively watches the core engine's
`transactions.authorized`/`transactions.declined` streams for monitoring.

This is a transparent rule-based scorer, not a trained ML model — see the
javadoc on `RiskScoringService` for why that distinction matters and what a
production version would need (labeled training data, a real model,
continuous retraining, device/IP signals). The core-processing-engine's
in-process velocity check still runs independently as a fast first-pass
filter; `fraud-risk-engine` is the deeper, callable service for a more
complete picture.

## Tokenization Vault

`tokenization-vault` (port 8084) is deliberately the **only** service that
ever handles a raw PAN. Every other service — core engine, switch, fraud
engine, CMS — works exclusively with vault-issued opaque tokens
(`tok_...`). This is the standard PCI-DSS pattern: shrink the "cardholder
data environment" down to one auditable, tightly-locked-down service instead
of scattering raw card numbers across the whole platform.

Encryption at rest uses AES-256-GCM — but the key currently lives in an
application config property, which is a demo-grade shortcut. A real vault
uses an HSM or managed KMS so the raw key never exists in application
memory; see `EncryptionService`'s javadoc for the full gap list.

## Card Management System (CMS)

`card-management` (port 8085) owns card issuance and lifecycle:
`ISSUED → ACTIVE → (BLOCKED ↔ ACTIVE) → CLOSED`, enforced as an explicit
state machine in `CardLifecycleService` — invalid transitions (e.g.
activating an already-active card, blocking a closed one) are rejected with
a clear error rather than silently allowed. Issuance calls the
tokenization-vault to get a token for a (synthetic, demo-only) PAN and never
touches the raw card number itself. PINs are hashed with bcrypt and the
plaintext PIN is never persisted.

## Clearing & Settlement

`clearing-settlement` (port 8086) consumes the core engine's
`transactions.authorized` Kafka stream continuously, recording each one to
an append-only ledger. A scheduled job (default: 23:59 daily, configurable
via cron) bundles all unsettled transactions into a `SettlementBatch` and
computes net positions per merchant. This demonstrates the netting/batching
pattern real settlement is built on — a full implementation would also
reconcile against the card network's own settlement files, handle
chargebacks/disputes that arrive after authorization, and produce actual
fund transfer instructions (ACH/wire) to move real money between issuer and
acquirer accounts.

## API Gateway

`api-gateway` (port 8080) is the single entry point for external callers
(banks, merchants, mobile apps), built on Spring Cloud Gateway. It routes
path prefixes (`/core/**`, `/switch/**`, `/fraud/**`, `/vault/**`,
`/cards/**`, `/settlement/**`) to the corresponding backend service, and
validates OAuth2 JWT bearer tokens on every route except `/actuator/health`.

Two things are explicitly NOT included, because they need infrastructure
beyond a single Spring Boot service:
- **A real OAuth2 authorization server.** The gateway validates tokens
  issued elsewhere — point `OAUTH2_ISSUER_URI` at Keycloak, Auth0, Okta, AWS
  Cognito, or similar. For local demo only, the `nosecurity` Spring profile
  disables validation entirely — never use it anywhere but your own machine.
- **mTLS termination.** Real bank/merchant integrations typically require
  mutual TLS (both sides present certificates), which is normally handled
  by a dedicated ingress/load balancer (e.g. an AWS NLB with mTLS, or
  Istio/Linkerd at the mesh layer) in front of the gateway, not by
  application code.

## End-to-end flow

```
Bank / Merchant / Mobile App
      │  OAuth2 bearer token + request
      ▼
┌─────────────┐
│ api-gateway │  validates JWT, routes by path
└──────┬──────┘
       │
       ▼
┌────────────────┐   issue/activate/PIN    ┌──────────────────┐   tokenize/detokenize   ┌────────────────────┐
│ card-management │ ───────────────────────▶│ (customer setup)  │────────────────────────▶│ tokenization-vault  │
└────────────────┘                          └──────────────────┘                          └────────────────────┘
       │
       │ (at transaction time)
       ▼
┌────────────────┐   routes to issuer   ┌──────────────────────┐
│  payment-switch  │ ────────────────────▶│ core-processing-engine │  balance/limit/status + in-process velocity check
└────────────────┘                       └──────────────────────┘
                                                    │
                                                    │ publishes to Kafka
                                                    ▼
                                          transactions.authorized / declined
                                                    │
                                      ┌─────────────┴─────────────┐
                                      ▼                             ▼
                          ┌───────────────────┐         ┌──────────────────────┐
                          │  fraud-risk-engine  │         │  clearing-settlement   │
                          │  (passive Kafka       │         │  nets positions,       │
                          │   monitoring today)   │         │  end-of-day batch      │
                          └───────────────────┘         └──────────────────────┘
```

**Note on fraud-risk-engine wiring**: today it's only wired as a passive Kafka
consumer (watches transactions after the fact) plus a standalone `/score`
endpoint any caller can invoke directly. It is **not yet called synchronously
by `payment-switch` or `core-processing-engine`** before a transaction is
approved — that's a real gap, not a design choice. Wiring the switch or core
engine to call `fraud-risk-engine`'s `/api/v1/fraud/score` before finalizing
the authorization decision is the natural next step to close this loop.

## What's still not built

- Real card network membership/connectivity (Visa/Mastercard/etc.) — this
  platform models the internal architecture pattern, not external network
  integration, which requires a certification process with the networks
  themselves.
- A real OAuth2 authorization server and mTLS-terminating ingress (see
  API Gateway section above).
- KYC/AML/sanctions screening tooling.
- A trained fraud ML model (currently rule-based).
- Multi-region active-active deployment — the K8s manifests deploy to one
  cluster; true multi-region needs cross-region data replication strategy
  (CockroachDB or Cassandra, as the original requirements mentioned) which
  is a significant infrastructure project on its own.
