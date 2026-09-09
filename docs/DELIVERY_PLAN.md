# End-to-End Delivery Plan

This branch is the integration track for making the reference platform runnable and verifiable.

## Gates

1. Local runtime: one-command startup and health verification.
2. User experience: browser-accessible demo and service status.
3. End-to-end flow: deterministic synthetic payment scenario.
4. Reliability: health checks, dependency readiness, graceful failures.
5. Security: demo-safe configuration and secret handling.
6. CI/CD: build, tests, smoke verification, container verification.
7. Deployment: reproducible deployment configuration and post-deploy health checks.

Each gate must be implemented, validated, and have its CI result inspected before it is considered complete.

## Verification principle

A successful compilation is not enough. A gate is complete only when the relevant runtime or integration verification passes.
