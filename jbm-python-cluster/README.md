# JBM Python Cluster

`jbm-python-cluster` is the Python migration workspace for JBM cluster services.
It follows the Java Maven management style: one application owns one root
directory, and each application keeps its own `src/`, `resource/`, and `tests/`.

## Layout

```text
jbm-python-cluster
├── common
│   ├── resource
│   ├── src/jbm_cluster_py/common
│   └── tests
├── integrations
│   ├── resource
│   └── src/jbm_cluster_py/integrations
├── auth
│   ├── resource
│   ├── src/jbm_cluster_py/platform/auth
│   └── tests
├── logs
│   ├── resource
│   ├── src/jbm_cluster_py/platform/logs
│   └── tests
├── doc
│   ├── resource
│   ├── src/jbm_cluster_py/platform/doc
│   └── tests
├── push
│   ├── resource
│   ├── src/jbm_cluster_py/platform/push
│   └── tests
└── job
    ├── resource
    ├── src/jbm_cluster_py/platform/job
    └── tests
```

## Run

Use Python 3.11 or newer. `uv` is preferred:

```bash
cd /opt/JBM/jbm-python-cluster
uv sync --extra dev
JBM_APP=auth JBM_PROFILE=jaja7 uv run python -m jbm_cluster_py.platform.auth.main
JBM_APP=logs JBM_PROFILE=jaja7 uv run python -m jbm_cluster_py.platform.logs.main
JBM_APP=doc JBM_PROFILE=jaja7 uv run python -m jbm_cluster_py.platform.doc.main
JBM_APP=push JBM_PROFILE=jaja7 uv run python -m jbm_cluster_py.platform.push.main
JBM_APP=job JBM_PROFILE=jaja7 uv run python -m jbm_cluster_py.platform.job.main
```

Without `uv`:

```bash
python -m venv .venv
. .venv/bin/activate
pip install -i https://mirrors.aliyun.com/pypi/simple/ --trusted-host mirrors.aliyun.com -r requirements.txt
PYTHONPATH=common/src:integrations/src:auth/src:logs/src:doc/src:push/src:job/src \
  JBM_APP=doc JBM_PROFILE=jaja7 python -m jbm_cluster_py.platform.doc.main
```

## Services

- `auth`: OAuth2/OIDC compatible replacement for `jbm-cluster-platform-auth`,
  port `5555`, paths `/oauth2/token`, `/oauth2/refresh`,
  `/oauth2/userinfo`, `/oauth2/logout`, `/.well-known/openid-configuration`,
  and `/jwks.json`.
- `logs`: compatible replacement for `jbm-cluster-platform-logs`, port `3312`,
  paths `/GatewayLogs/**`, `/clusterAccess/**`, `/businessLog/**`.
- `doc`: compatible replacement for `jbm-cluster-platform-doc`, port `9999`,
  paths `/put`, `/upload`, `/get/**`, `/download/**`, `/baseDoc/**`,
  `/baseDocGroup/**`, `/baseDocToken/**`, `/v1/3rd/file/**`.
- `push`: compatible replacement for `jbm-cluster-platform-push`, port `3313`,
  paths `/pushMessage/**`, `/pushTest/**`, `/pushConfigInfo/**`,
  `/emailPushConfig/**`, and STOMP-compatible `/ws`.
- `job`: compatible replacement for `jbm-cluster-platform-job`, port `4444`,
  paths `/sysJob/**` and `/sysJobLog/**`.

Both services expose `/actuator/health`, `/openapi.json`, and `/docs`.

## Configuration

Configuration keeps a Spring Boot YAML profile shape and is loaded in order:

1. `common/resource/application.yml`
2. `common/resource/application-{profile}.yml`
3. `integrations/resource/application.yml`
4. `integrations/resource/application-{profile}.yml`
5. `{app}/resource/application.yml`
6. `{app}/resource/application-{profile}.yml`

`JBM_APP=auth|logs|doc|push|job` selects the application. `JBM_PROFILE=jaja7` selects the
profile. Nacos shared dataids such as `common.properties`, `db.properties`,
`redis.properties`, `rabbitmq.properties`, and `doc.properties` are preserved in
the profile configuration for cluster alignment.

## Docker

The image uses the DaoCloud accelerated Python base image and Aliyun apt/pip
sources.

```bash
docker build -t jbm-cluster-platform-doc:7.3-py .
docker run --rm -e JBM_APP=doc -e JBM_PROFILE=jaja7 -p 9999:9999 jbm-cluster-platform-doc:7.3-py
```

## Migration Notes

- This project is intentionally not added to the root Maven `pom.xml`.
- Java services can keep running while Python images are tested service by service.
- The auth implementation issues standard RS256 JWT access tokens and exposes
  JWKS for downstream Java Sa-Token standard-JWT verification.
- The doc implementation stores metadata with Java-compatible field names and
  uses S3/MinIO by default in cluster profiles, with filesystem fallback for
  local development and tests.
