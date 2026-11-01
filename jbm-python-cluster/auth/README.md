# JBM Python Auth

Python replacement for `jbm-cluster-platform-auth`.

The optional standard issuer now provides authorization-code OIDC and browser
SSO at `/oidc/*`. Existing `/oauth2/*` routes keep their JBM response contract.
See [standardization and rollout](../docs/auth-standardization.md) for registration,
schema migration, key rotation, test commands, and the supported protocol subset.

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

Outside explicit `dev`, `test`, `local`, and `default` profiles, startup requires
a stable `jbm.auth.jwt.private-key`, HTTPS issuer, and available shared Redis.
Temporary keys and per-process state are restricted to those development profiles.
`GET /actuator/health/readiness` checks the live database and session store.

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

## WeChat Mini Program phone login

The native `getPhoneNumber` button supplies a one-use phone authorization code.
`POST /oauth2/doLogin` accepts `loginType=MINIAPP`, `wx_app_id` and `wx_phone_code`
alongside the normal authorization-code + S256 PKCE parameters. The server uses
WeChat's stable access token and `getuserphonenumber` API to verify the phone.
This is independent of `wx.login` codes; never substitute a client-supplied phone.
Each fresh login requires an authorized phone code; persistent OpenID linking is
not part of this flow. Normal token refresh preserves the original auth time.

Configure the **server-side** `jbm.auth.login-providers.miniapp` in the existing
Nacos auth configuration (or its protected `JBM_CONFIG_JSON` override):

```yaml
enabled: true
mode: wechat-phone
dev-mock-enabled: false
app-id: wxbf2072314bea5b63
app-secret: "<server-side secret; never commit or ship in the Mini Program>"
# Alternatively mount a Docker Secret and use app-secret-file instead:
# app-secret-file: /run/secrets/jbm_wechat_phone
client-ids:
  - iot_platform-98dc2cbb2a528b0e:miniapp
```

Only a unique existing active user with the verified mainland-China mobile
number can sign in. Missing mobile login credentials are bound using the existing
transactional mobile binding; unknown users are never auto-registered. Conflicting
ownership and disabled mobile credentials are rejected. Roles and tenant/project
access come from the existing account and normal JBM session pipeline.

`GET /oauth2/miniapp/options?client_id=...&app_id=...` exposes readiness and a
user-facing message, never the secret or upstream access token. Until configured,
the Mini Program displays the entry and its unavailable reason with password
login retained. Enable the phone capability/privacy declarations and HTTPS server
domain in the WeChat console before real-device testing. Development mocks do
not count as real WeChat verification. No changes to batch-control reauthentication
policy or automatic replay of previous controls are included.

Authenticated users can bind their own phone from the Mini Program's My page:
`POST /oauth2/miniapp/mobile/bind`, with the same `wx_app_id` and fresh
`wx_phone_code`, plus the normal bearer token. The account comes exclusively from
the verified session, never from a request user ID. Recent account authentication
is required using the existing policy. The verified phone is saved through the
existing transactional mobile binding, with a first-binding guard checked under
the user row lock. Conflicting ownership, disabled mobile credentials and replacing
an existing different phone are rejected. Existing platform SMS-based phone-change
flows are retained. Invalid WeChat grants return 400 and do not invalidate the
account login. The response contains only `mobileBound` and a masked phone.

### Multiple Mini Programs and deployment environments

An Auth deployment can configure multiple WeChat AppIDs. Select an application
by the exact runtime `wx_app_id` and validate its allowed JBM OAuth client IDs:

```yaml
jbm:
  auth:
    login-providers:
      miniapp:
        enabled: true
        mode: wechat-phone
        dev-mock-enabled: false
        applications:
          wx_first_app_id:
            client-ids: [first_registered_jbm_client:miniapp]
            app-secret-file: /run/secrets/wechat_first
          wx_second_app_id:
            client-ids: [second_registered_jbm_client:miniapp]
            app-secret-file: /run/secrets/wechat_second
```

Use actual registered client IDs and AppIDs; the names above are examples, not
implicit registrations. Prefer separate JBM client registrations for separate
Mini Programs so grants, sessions and audit records can be managed per client.
For independent concurrent sessions, register distinct JBM applications (different
JBM appId/clientId). The current JBM session index is userId + JBM appId + terminal:
sharing one application's miniapp terminal retains its same-terminal replacement
policy, even when the WeChat AppIDs differ. This change does not override that policy.
Each entry has its own server-only secret and client allowlist. Upstream access
tokens are cached by AppID. A grant's watermark must match the selected AppID.
An unknown AppID, disabled entry, missing secret or unlisted client is rejected;
there is no first-entry/default-credential fallback. An `applications` map, even
when empty, takes precedence over the legacy single-AppID fields.

Each platform environment supplies its own Nacos configuration namespace, secrets,
JBM client registrations, issuer/signing keys and session storage (or distinct
Redis key prefix). Do not reuse another environment's login keys or secrets.
The Mini Program's `config/index.js` supplies that release package's HTTPS platform
origin and JBM `clientId`; `project.config.json` supplies its WeChat AppID, and
WeChat's server-domain/privacy capabilities must be configured for that AppID.
The backend does not hardcode an environment or a WeChat AppID. The runtime AppID
selects credentials inside the contacted platform; it never selects an arbitrary
remote platform. This is multiple Mini Programs per deployment, not a client-side
platform-switching feature. Configuration is loaded at Auth startup: apply changed
configuration and secret mounts through the normal deployment/restart workflow.
