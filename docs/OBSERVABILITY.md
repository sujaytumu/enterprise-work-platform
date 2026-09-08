# Observability

All 7 services ship with production-shaped observability out of the box:
**metrics** (Prometheus), **distributed tracing** (Zipkin), and **centralized
structured logs** (Loki), correlated by trace ID across all three.

## Running it

The base `docker-compose.yml` stays lean for day-to-day work. Opt into the
full observability stack with an overlay file:

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml up --build
```

Then open:
- **Grafana** — http://localhost:3000 (login `admin` / `admin`) — pre-provisioned
  with Prometheus, Zipkin, and Loki as data sources, plus a starter
  "Payment Platform Overview" dashboard (request rate, error rate, p95
  latency, JVM heap per service).
- **Prometheus** — http://localhost:9090 — raw metrics + query UI.
- **Zipkin** — http://localhost:9411 — search traces by service, look at the
  full call graph of a single request across services.

## How each piece is wired

### Metrics (Prometheus)
Every service exposes `/actuator/prometheus` via `micrometer-registry-prometheus`
(added in each service's `pom.xml`). Out of the box this gives you, per
service, with zero extra code: HTTP request rate/latency/status broken down
by endpoint, JVM heap/GC/thread pool stats, and datasource connection pool
usage. `observability/prometheus/prometheus.yml` scrapes all 7 every 10s.

**Going further**: the dashboard's "Authorization Decisions" and "Fraud
Alerts" panels currently reuse the generic HTTP request metrics as a stand-in.
For real business metrics (e.g. a counter that increments specifically on
`APPROVED` vs `DECLINED`, tagged by decline reason), inject a
`MeterRegistry` and add custom counters — for example, in
`AuthorizationService`:
```java
private final Counter approvedCounter;
// in constructor: approvedCounter = meterRegistry.counter("authorizations.approved");
// in authorize(): if (decision.approved()) approvedCounter.increment();
```
This is the natural next step to make the dashboard tell a business story,
not just an infrastructure one.

### Distributed tracing (Zipkin)
`micrometer-tracing-bridge-brave` + `zipkin-reporter-brave` auto-instrument
every incoming/outgoing HTTP call and Kafka publish once
`management.tracing.sampling.probability: 1.0` is set (already in every
`application.yml`; dial this down from 1.0 in a real deployment — sampling
100% of traces is fine for a demo, expensive at real transaction volume).
A request through `api-gateway → payment-switch → core-processing-engine`
shows up in Zipkin as a single trace with one span per service, so you can
see exactly where time went on a slow request.

### Centralized logs (Loki + Promtail)
Each service can emit JSON logs (via `logstash-logback-encoder`,
`logback-spring.xml` in each service) instead of plain text, activated by
the `json-logs` Spring profile (already wired in the observability compose
overlay). Promtail tails Docker container logs, parses the JSON, and ships
to Loki with `level` and `traceId` as queryable labels. In Grafana's Explore
view, you can filter logs by service and log level, or paste a trace ID from
Zipkin to see every log line from every service for that one request.

## What's demo-grade vs. production-grade here

- **Sampling 100% of traces** — fine for local dev, not for real volume.
  Real deployments sample a small percentage (often <5%) plus always-sample
  on errors.
- **Grafana admin/admin** — change this immediately if this ever runs
  anywhere but your own machine; see `docker-compose.observability.yml`.
- **Loki filesystem storage, single Prometheus instance** — fine locally;
  a real deployment uses managed/clustered versions (Grafana Cloud,
  Amazon Managed Prometheus, etc.) or a proper HA setup with durable storage.
- **No alerting configured** — Prometheus Alertmanager / Grafana alerting
  rules (e.g. "page someone if the decline rate spikes above X%") are a
  natural next addition, not included here.
