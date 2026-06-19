# JBM Python Auth

Python replacement for `jbm-cluster-platform-auth`.

## Scope

- OAuth2 password grant: `POST /oauth2/token`
- OAuth2 client credentials: `POST /oauth2/token`
- Refresh token: `POST /oauth2/refresh`
- User info: `GET|POST /oauth2/userinfo`
- Logout and access-token revocation marker: `POST|DELETE /oauth2/logout`
- OIDC discovery: `GET /.well-known/openid-configuration`
- JWKS: `GET /jwks.json`
- Java frontend password encryption compatibility: `GET /oauth2/publicKey` plus RSA encrypted password submit.

## Data Compatibility

The service reads the existing JBM tables:

- `base_app` / `base_api_key` for OAuth clients and RSA login keys.
- `base_account` / `base_user` for password login.
- `base_role`, `base_role_user`, `base_authority_role`, `base_authority_user`, `base_authority` for JWT roles and permissions.

Stored user passwords and client secrets support Java BCrypt values. Plaintext is accepted only for local fixtures or legacy rows.

## Downstream Java Sa-Token

The Python service issues standard RS256 JWT access tokens. Java services must enable the standard JWT bridge added under `jbm-cluster-common-satoken`:

```properties
jbm.security.standard-jwt.enabled=true
jbm.security.standard-jwt.issuer=http://jbm-cluster-platform-auth:5555
jbm.security.standard-jwt.audience=jbm-api
jbm.security.standard-jwt.jwks-uri=http://jbm-cluster-platform-auth:5555/jwks.json
jbm.security.standard-jwt.accepted-algorithms[0]=RS256
```

For production, configure `jbm.auth.jwt.private-key` with a stable RSA private key. If it is empty, auth generates an in-memory key on startup, which is useful for tests but not for a running cluster.
