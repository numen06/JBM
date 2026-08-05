# JBM Python Cluster

`jbm-python-cluster` is the Python runtime for JBM 7.3 cluster services. Each
application owns one root directory with `src/`, `resource/`, and `tests/`.

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
├── gateway
├── center
├── logs
├── bigscreen
├── doc
│   ├── resource
│   ├── src/jbm_cluster_py/platform/doc
│   └── tests
├── push
│   ├── resource
│   ├── src/jbm_cluster_py/platform/push
│   └── tests
└── job
```

## Run

Use Python 3.11 or newer. `uv` is preferred:

```bash
cd /opt/JBM/jbm-python-cluster
uv sync --extra dev
JBM_APP=auth JBM_PROFILE=jaja7 uv run python -m jbm_cluster_py.platform.auth.main
JBM_APP=gateway JBM_PROFILE=jaja7 uv run python -m jbm_cluster_py.platform.gateway.main
JBM_APP=doc JBM_PROFILE=jaja7 uv run python -m jbm_cluster_py.platform.doc.main
JBM_APP=push JBM_PROFILE=jaja7 uv run python -m jbm_cluster_py.platform.push.main
JBM_APP=job JBM_PROFILE=jaja7 uv run python -m jbm_cluster_py.platform.job.main
JBM_APP=center JBM_PROFILE=jaja7 uv run python -m jbm_cluster_py.platform.center.main
JBM_APP=logs JBM_PROFILE=jaja7 uv run python -m jbm_cluster_py.platform.logs.main
JBM_APP=bigscreen JBM_PROFILE=jaja7 uv run python -m jbm_cluster_py.platform.bigscreen.main
```

Without `uv`:

```bash
python -m venv .venv
. .venv/bin/activate
pip install -i https://mirrors.aliyun.com/pypi/simple/ --trusted-host mirrors.aliyun.com -r requirements.txt
PYTHONPATH=common/src:integrations/src:auth/src:center/src:gateway/src:doc/src:push/src:job/src \
  JBM_APP=doc JBM_PROFILE=jaja7 python -m jbm_cluster_py.platform.doc.main
```

## Services

- `auth`: OAuth2/OIDC compatible replacement for `jbm-cluster-platform-auth`,
  port `5555`, paths `/oauth2/token`, `/oauth2/refresh`,
  `/oauth2/userinfo`, `/oauth2/logout`, `/.well-known/openid-configuration`,
  and `/jwks.json`.
- `gateway`: unified HTTP/WebSocket entry, dynamic routes and traffic policies,
  port `6060`; gateway access-log persistence, statistics, and filter rules are
  integrated here to keep request capture at the source.
- `logs`: business-log persistence, stage progress, SSE, signed downloads, and
  RabbitMQ event consumption, port `3312`. Login audit remains owned by Center.
- `bigscreen`: big-screen metadata and secure static ZIP deployment, port `3314`.
- `doc`: compatible replacement for `jbm-cluster-platform-doc`, port `9999`,
  paths `/put`, `/upload`, `/get/**`, `/download/**`, `/baseDoc/**`,
  `/baseDocGroup/**`, `/baseDocToken/**`, `/v1/3rd/file/**`.
- `push`: compatible replacement for `jbm-cluster-platform-push`, port `3313`,
  paths `/pushMessage/**`, `/pushTest/**`, `/pushConfigInfo/**`,
  `/emailPushConfig/**`, and STOMP-compatible `/ws`.
- `job`: compatible replacement for `jbm-cluster-platform-job`, port `4444`,
  paths `/sysJob/**` and `/sysJobLog/**`.
- `center`: complete governance service, port `7777`, with Alembic schema and
  idempotent baseline seed.

All services expose `/actuator/health`, `/openapi.json`, and `/docs`.

## Configuration

Configuration keeps a Spring Boot YAML profile shape and is loaded in order:

1. `common/resource/application.yml`
2. `common/resource/application-{profile}.yml`
3. `integrations/resource/application.yml`
4. `integrations/resource/application-{profile}.yml`
5. `{app}/resource/application.yml`
6. `{app}/resource/application-{profile}.yml`

`JBM_APP=auth|center|gateway|doc|push|job|logs|bigscreen` selects the application. `JBM_PROFILE=jaja7` selects the
profile. Compose disables remote configuration loading and uses r-nacos only for service discovery;
database, Redis, RabbitMQ, and MinIO settings are supplied through environment variables.

## Docker

The image uses the DaoCloud accelerated Python base image and Aliyun apt/pip
sources.

```bash
docker build -t jbm-cluster-platform-doc:7.3-py .
docker run --rm -e JBM_APP=doc -e JBM_PROFILE=jaja7 -p 9999:9999 jbm-cluster-platform-doc:7.3-py
```

The auth implementation issues RS256 JWT access tokens and exposes JWKS. Doc
uses S3/MinIO in Compose with a filesystem fallback for development and tests.

## CodeGraph

The branch includes repository-level CodeGraph instructions and a tracked
`.codegraph/.gitignore`. Initialize the machine-local index after cloning:

```bash
codegraph init .
codegraph explore "How does a gateway request reach a Python platform service?"
```

The generated database stays local and is never committed.
