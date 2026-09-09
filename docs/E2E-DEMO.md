# End-to-end demo

This script exercises the local reference implementation through the API gateway using **synthetic data only**:

1. Issue a demo card.
2. Activate it.
3. Tokenize a synthetic PAN.
4. Create a demo account.
5. Submit a payment authorization through the payment switch.

Start the stack first:

```bash
docker compose up --build
```

Then run:

```bash
bash scripts/e2e-demo.sh
```

The script intentionally uses a dummy PAN and local demo security settings. It is not a real-money or real-card workflow.
