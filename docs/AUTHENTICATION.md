# Authentication and Credential Flow

## Components

- **API Gateway (8080)**: public entry point and OAuth2 resource server.
- **OAuth2/OIDC provider**: external issuer such as Keycloak, Auth0, Okta, or Cognito. It authenticates users and signs JWTs; this repository does not contain an authorization server.
- **Backend services**: core processing, payment switch, fraud, vault, card management, and settlement. The gateway routes requests to them.
- **Tokenization Vault**: handles synthetic demo PANs and stores only encrypted PAN data. Its vault token (`tok_...`) is a payment-data token, not a login credential.

## Request flow

1. A client authenticates with the external OAuth2/OIDC provider.
2. The provider returns an access token (JWT).
3. The client sends `Authorization: Bearer <JWT>` to the API Gateway.
4. Spring Security's resource-server support validates the JWT against the configured `OAUTH2_ISSUER_URI`.
5. If validation succeeds, Spring Cloud Gateway selects a route by path prefix and forwards the request.
6. Backend services perform business processing. Card-management uses bcrypt hashes for PINs; the vault encrypts demo PANs with AES-GCM and exposes opaque `tok_...` values to other services.

## Configuration

The issuer URL is supplied through the environment variable `OAUTH2_ISSUER_URI`. Do not commit issuer secrets, client secrets, database passwords, or vault encryption keys. Deployment credentials belong in environment variables or a secret manager/KMS.

## Important distinction

- **JWT access token**: authenticates/authorizes an API caller.
- **Vault token**: replaces a PAN for payment-data isolation.
- **PIN hash**: one-way credential representation; plaintext PINs are not persisted.

## Local development

The repository documents a `nosecurity` profile for local-only demonstrations. It must never be enabled on an internet-accessible deployment. Production requires a real issuer, TLS, secret management, and additional controls such as mTLS and least-privilege service identities.
