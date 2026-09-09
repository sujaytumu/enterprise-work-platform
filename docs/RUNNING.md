# Running the platform

## Start

```bash
docker compose up --build
```

Open http://localhost:3000.

## Verify

```bash
chmod +x scripts/smoke-test.sh
./scripts/smoke-test.sh
```

The smoke test checks all seven Spring Boot health endpoints and the client response.

## Stop

```bash
docker compose down
```
