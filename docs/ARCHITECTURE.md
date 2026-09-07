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

## Fraud & Risk Engine (built)

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
complete picture. Wiring the switch/core engine to call
`fraud-risk-engine` synchronously before authorizing is a natural next step.

## Next phases

1. **Tokenization Vault** — replace PAN with a vault-issued token at ingress;
   downstream services never see the real PAN.
2. **Card Management System** — card issuance, activation, PIN set (hashed,
   never stored plaintext), lifecycle state machine.
3. **Clearing & Settlement** — batch job that nets positions between issuer
   and acquirer accounts at end of day, from the `transactions.authorized`
   Kafka log.
4. **API Gateway** — OAuth2 client-credentials for bank/merchant callers,
   mTLS termination, rate limiting.
