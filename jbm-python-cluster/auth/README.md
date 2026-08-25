# JBM Python Auth

Python replacement for `jbm-cluster-platform-auth`.

## Scope

- OAuth2 authorization code + PKCE (S256): `GET|POST /oauth2/authorize`, `POST /oauth2/token`
- OAuth2 client credentials: `POST /oauth2/token`
- Refresh-token rotation and replay detection: `POST /oauth2/token`
- Token revocation and introspection: `POST /oauth2/revoke`, `POST /oauth2/introspect`
- User info: `GET|POST /oauth2/userinfo`
- Logout and access-token revocation marker: `POST|DELETE /oauth2/logout`
- OAuth authorization-server metadata: `GET /.well-known/oauth-authorization-server`
- Legacy metadata alias: `GET /.well-known/openid-configuration`
- JWKS: `GET /jwks.json`
- Java frontend password encryption compatibility: `GET /oauth2/publicKey` plus RSA encrypted password submit.

## Data Compatibility

The service reads the existing JBM tables:

- `base_app` / `base_api_key` for OAuth clients and RSA login keys.
- `base_account` / `base_user` for password login.
- `base_role`, `base_role_user`, `base_authority_role`, `base_authority_user`, `base_authority` for JWT roles and permissions.

Stored user passwords and client secrets support BCrypt and the existing encrypted client-secret format. Plaintext secrets are rejected by default; test fixtures must explicitly enable `jbm.auth.allow-plaintext-secrets`.

## Downstream services

The Python service issues RS256 JWT access tokens. Cluster services validate each request through `/oauth2/userinfo`, so refresh rotation, logout, manual kick-out and revocation take effect across services immediately.

For production, configure `jbm.auth.jwt.private-key` with a stable RSA private key. If it is empty, auth generates an in-memory key on startup, which is useful for tests but not for a running cluster.

## Alibaba Cloud SMS verification

SMS login and registration reuse `POST /captcha/pcode` for sending and `POST /captcha/pcode/verify` for verification. Auth owns the authentication flow; the Push service owns SMS providers and the Alibaba Cloud SDK. Auth only keeps the following policy in `auth-{profile}.yml` on Nacos:

```yaml
jbm:
  auth:
    sms:
      registration-required: true
      push-service: jbm-cluster-platform-push
      valid-time: 300
      interval: 60
```

Configure `aliyun.sms` in Push's `push-{profile}.yml` on Nacos. The RAM identity needs `dypns:SendSmsVerifyCode` and `dypns:CheckSmsVerifyCode`. Push always sends the dynamic `##code##` placeholder and only returns verified when `Model.VerifyResult=PASS`.

```yaml
aliyun:
  sms:
    verificationProvider: aliyun-pnvs
    accessKeyId: "<Nacos secret>"
    accessKeySecret: "<Nacos secret>"
    # securityToken: "<Nacos STS token>"
    verificationSignName: "<system sign>"
    verificationTemplateCode: "<system template code>"
    schemeName: "<optional scheme>"
    countryCode: "86"
    codeLength: 6
    validTime: 300
    interval: 60
```

Nacos is authoritative. The Push database channel row only receives non-sensitive display defaults; AccessKey, Secret and STS Token must never be copied to `push_config_info` or returned by the channel-management API.
