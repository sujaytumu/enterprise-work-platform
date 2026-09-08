# Kubernetes deployment

## Prerequisites

- A Kubernetes cluster
- Kafka and PostgreSQL reachable from the cluster
- Container images pushed to a registry
- A real secret created outside git

## Deploy

1. Replace every `REPLACE_ME/<service>:latest` image with an image you control.
2. Do **not** apply the sample Secret values in `config.yaml`. Create a real secret:

```bash
kubectl create secret generic payments-db-secret \
  --from-literal=DB_USER=payments \
  --from-literal=DB_PASSWORD='replace-with-real-secret' \
  --from-literal=VAULT_ENCRYPTION_KEY="$(openssl rand -base64 32)"
```

3. Configure a real OAuth2 issuer.
4. Apply:

```bash
kubectl apply -k deploy/k8s
kubectl rollout status deployment/api-gateway
```

The included manifests are a deployment scaffold. A live cloud deployment still requires choosing a cluster and image registry and supplying their credentials.
