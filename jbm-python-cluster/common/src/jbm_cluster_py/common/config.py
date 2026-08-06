import copy
import json
import logging
import os
from pathlib import Path
from typing import Any, Dict, Iterable, List, Mapping, Optional

import yaml

logger = logging.getLogger(__name__)


def deep_merge(base: Mapping[str, Any], override: Mapping[str, Any]) -> Dict[str, Any]:
    merged = copy.deepcopy(dict(base))
    for key, value in override.items():
        if isinstance(value, Mapping) and isinstance(merged.get(key), Mapping):
            merged[key] = deep_merge(merged[key], value)
        else:
            merged[key] = copy.deepcopy(value)
    return merged


def nested_get(data: Mapping[str, Any], keys: Iterable[str], default: Any = None) -> Any:
    current = data
    for key in keys:
        if not isinstance(current, Mapping) or key not in current:
            return default
        current = current[key]
    return current


def nested_set(data: Dict[str, Any], dotted_key: str, value: Any) -> None:
    keys = [key for key in dotted_key.split(".") if key]
    if not keys:
        return
    current: Dict[str, Any] = data
    for key in keys[:-1]:
        child = current.get(key)
        if not isinstance(child, dict):
            child = {}
            current[key] = child
        current = child
    current[keys[-1]] = value


def parse_properties(content: str) -> Dict[str, Any]:
    result: Dict[str, Any] = {}
    for line in content.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or stripped.startswith("!"):
            continue
        separator = "=" if "=" in stripped else ":"
        if separator not in stripped:
            continue
        key, value = stripped.split(separator, 1)
        nested_set(result, key.strip(), _coerce_scalar(value.strip()))
    return result


def load_yaml_file(path: Path) -> Dict[str, Any]:
    if not path.exists():
        return {}
    with path.open("r", encoding="utf-8") as handle:
        return yaml.safe_load(handle) or {}


def _coerce_scalar(value: str) -> Any:
    lowered = value.lower()
    if lowered == "true":
        return True
    if lowered == "false":
        return False
    if lowered in {"null", "none"}:
        return None
    try:
        if value.isdigit() or (value.startswith("-") and value[1:].isdigit()):
            return int(value)
    except Exception:
        pass
    return value


def _load_config_content(data_id: str, content: str) -> Dict[str, Any]:
    if data_id.endswith((".yml", ".yaml")):
        return yaml.safe_load(content) or {}
    return parse_properties(content)


def _env_overrides() -> Dict[str, Any]:
    overrides: Dict[str, Any] = {}
    json_override = os.getenv("JBM_CONFIG_JSON")
    if json_override:
        try:
            loaded = json.loads(json_override)
            if isinstance(loaded, Mapping):
                overrides = deep_merge(overrides, loaded)
        except json.JSONDecodeError as exc:
            logger.warning("Ignoring invalid JBM_CONFIG_JSON: %s", exc)
    env_map = {
        "JBM_DATABASE_URL": "integrations.database.url",
        "JBM_REDIS_URL": "integrations.redis.url",
        "JBM_RABBITMQ_URL": "integrations.rabbitmq.url",
        "JBM_KAFKA_BOOTSTRAP_SERVERS": "integrations.kafka.bootstrap-servers",
        "JBM_LOKI_URL": "jbm.logs.loki.url",
        "JBM_NACOS_SERVER_ADDR": "spring.cloud.nacos.discovery.server-addr",
        "JBM_AUTH_JWT_PRIVATE_KEY": "jbm.auth.jwt.private-key",
        "JBM_AUTH_JWT_ISSUER": "jbm.auth.jwt.issuer",
        "JBM_AUTH_JWT_AUDIENCE": "jbm.auth.jwt.audience",
        "JBM_AUTH_JWT_KEY_ID": "jbm.auth.jwt.key-id",
        "JBM_GATEWAY_SERVICE_TOKEN_SERVICE": "jbm.gateway.service-token.service",
        "JBM_GATEWAY_SERVICE_TOKEN_PATH": "jbm.gateway.service-token.path",
        "JBM_GATEWAY_SERVICE_TOKEN_CLIENT_ID": "jbm.gateway.service-token.client-id",
        "JBM_GATEWAY_SERVICE_TOKEN_CLIENT_SECRET": "jbm.gateway.service-token.client-secret",
        "JBM_GATEWAY_SERVICE_TOKEN_SCOPE": "jbm.gateway.service-token.scope",
    }
    for env_name, dotted_key in env_map.items():
        value = os.getenv(env_name)
        if value:
            nested_set(overrides, dotted_key, value)
    return overrides


class AppConfig:
    """Small Spring-style YAML config wrapper.

    The Java side uses dotted Spring keys and profile-specific bootstrap files.
    This wrapper keeps that shape intact while giving Python code typed accessors.
    """

    def __init__(
        self,
        raw: Mapping[str, Any],
        profile: str,
        config_dir: Optional[Path],
        app: Optional[str] = None,
        resource_dir: Optional[Path] = None,
        resource_dirs: Optional[List[Path]] = None,
    ) -> None:
        self.raw = dict(raw)
        self.profile = profile
        self.config_dir = config_dir
        self.app = app
        self.resource_dir = resource_dir
        self.resource_dirs = resource_dirs or ([resource_dir] if resource_dir else [])

    @classmethod
    def load(
        cls,
        profile: Optional[str] = None,
        config_dir: Optional[Path] = None,
        app: Optional[str] = None,
        resource_dir: Optional[Path] = None,
    ) -> "AppConfig":
        selected_profile = profile or os.getenv("JBM_PROFILE") or os.getenv("ENV") or "jaja7"
        selected_app = app or os.getenv("JBM_APP") or None
        base_dir = config_dir or cls.default_config_dir()
        app_resource_dir = resource_dir or cls.default_resource_dir(selected_app)
        resource_dirs = cls.default_resource_dirs(selected_app, base_dir, app_resource_dir)
        merged: Dict[str, Any] = {}
        for current_dir in resource_dirs:
            merged = deep_merge(merged, load_yaml_file(current_dir / "application.yml"))
            merged = deep_merge(
                merged,
                load_yaml_file(current_dir / ("application-%s.yml" % selected_profile)),
            )
        env_overrides = _env_overrides()
        # Environment values must be visible before contacting Nacos so local/container
        # profiles can disable or redirect config loading. Reapply them afterwards so
        # deployment-time settings remain authoritative over remote shared config.
        merged = deep_merge(merged, env_overrides)
        merged = cls._merge_nacos_shared_config(merged)
        merged = deep_merge(merged, env_overrides)
        return cls(merged, selected_profile, base_dir, selected_app, app_resource_dir, resource_dirs)

    @staticmethod
    def _merge_nacos_shared_config(config: Mapping[str, Any]) -> Dict[str, Any]:
        nacos_config = dict(nested_get(config, ["spring", "cloud", "nacos", "config"], {}) or {})
        if nacos_config.get("enabled") is False:
            return dict(config)
        shared_data_ids = nacos_config.get("shared-dataids") or nacos_config.get("sharedDataids")
        if not shared_data_ids:
            return dict(config)
        server_addr = nacos_config.get("server-addr") or nacos_config.get("serverAddr")
        if not server_addr:
            return dict(config)
        try:
            import nacos
        except ImportError:
            logger.warning("nacos-sdk-python is not installed; skip shared config loading")
            return dict(config)

        namespace = str(nacos_config.get("namespace") or "public")
        group = str(nacos_config.get("group") or "DEFAULT_GROUP")
        data_ids = [item.strip() for item in str(shared_data_ids).split(",") if item.strip()]
        try:
            client = nacos.NacosClient(server_addresses=str(server_addr), namespace=namespace)
        except Exception as exc:
            logger.warning("Nacos config client creation failed; skip shared config loading: %s", exc)
            return dict(config)

        merged = dict(config)
        for data_id in data_ids:
            try:
                content = client.get_config(data_id, group)
            except Exception as exc:
                logger.warning("Failed to load Nacos config %s/%s: %s", group, data_id, exc)
                continue
            if not content:
                continue
            try:
                merged = deep_merge(merged, _load_config_content(data_id, str(content)))
            except Exception as exc:
                logger.warning("Failed to parse Nacos config %s: %s", data_id, exc)
        return merged

    @staticmethod
    def project_root() -> Path:
        return Path(__file__).resolve().parents[4]

    @staticmethod
    def default_config_dir() -> Path:
        configured = os.getenv("JBM_CONFIG_DIR")
        if configured:
            return Path(configured)
        return AppConfig.project_root() / "common" / "resource"

    @staticmethod
    def default_resource_dir(app: Optional[str]) -> Optional[Path]:
        configured = os.getenv("JBM_RESOURCE_DIR")
        if configured:
            return Path(configured)
        if not app:
            return None
        return AppConfig.project_root() / app / "resource"

    @staticmethod
    def default_resource_dirs(
        app: Optional[str],
        base_dir: Optional[Path],
        app_resource_dir: Optional[Path],
    ) -> List[Path]:
        root = AppConfig.project_root()
        candidates: List[Optional[Path]] = [
            root / "config",
            base_dir,
            root / "integrations" / "resource",
            app_resource_dir,
        ]
        result: List[Path] = []
        seen = set()
        for candidate in candidates:
            if candidate is None:
                continue
            resolved = candidate.resolve()
            if resolved in seen or not candidate.exists():
                continue
            seen.add(resolved)
            result.append(candidate)
        return result

    def get(self, dotted_key: str, default: Any = None) -> Any:
        return nested_get(self.raw, dotted_key.split("."), default)

    @property
    def host(self) -> str:
        return str(self.get("server.host", "0.0.0.0"))

    @property
    def port(self) -> int:
        return int(self.get("server.port", 3312))

    @property
    def service_name(self) -> str:
        return str(self.get("spring.application.name", "jbm-python-cluster"))

    @property
    def openapi(self) -> Dict[str, Any]:
        return dict(self.get("jbm.python.openapi", {}) or {})

    @property
    def nacos_discovery(self) -> Dict[str, Any]:
        return dict(self.get("spring.cloud.nacos.discovery", {}) or {})

    @property
    def nacos_config(self) -> Dict[str, Any]:
        return dict(self.get("spring.cloud.nacos.config", {}) or {})

    @property
    def database(self) -> Dict[str, Any]:
        spring_datasource = dict(self.get("spring.datasource", {}) or {})
        integration_config = dict(self.get("integrations.database", {}) or {})
        return deep_merge(spring_datasource, integration_config)

    @property
    def minio(self) -> Dict[str, Any]:
        config = dict(self.get("integrations.minio", {}) or {})
        spring_minio = dict(self.get("spring.minio", {}) or {})
        if spring_minio:
            mapped = {
                "endpoint-url": (
                    spring_minio.get("endpoint-url")
                    or spring_minio.get("endpointUrl")
                    or spring_minio.get("url")
                ),
                "bucket": spring_minio.get("bucket"),
                "access-key": spring_minio.get("access-key") or spring_minio.get("accessKey"),
                "secret-key": spring_minio.get("secret-key") or spring_minio.get("secretKey"),
                "secure": spring_minio.get("secure"),
                "enabled": True,
            }
            config = deep_merge(config, {key: value for key, value in mapped.items() if value is not None})
        return config

    @property
    def storage(self) -> Dict[str, Any]:
        return dict(self.get("integrations.storage", {}) or {})

    @property
    def wps(self) -> Dict[str, Any]:
        return dict(self.get("wps", {}) or {})

    @property
    def redirect(self) -> Dict[str, Any]:
        return dict(self.get("redirect", {}) or {})

    @property
    def openobserve(self) -> Dict[str, Any]:
        config = dict(self.get("integrations.openobserve", {}) or {})
        legacy = dict(self.get("open-observe", {}) or {})
        if legacy:
            mapped = {
                "url": legacy.get("url"),
                "org": legacy.get("organization") or legacy.get("org"),
                "stream": legacy.get("stream"),
                "username": legacy.get("username"),
                "password": legacy.get("password"),
            }
            config = deep_merge(config, {key: value for key, value in mapped.items() if value is not None})
        return config

    @property
    def rabbitmq(self) -> Dict[str, Any]:
        config = dict(self.get("integrations.rabbitmq", {}) or {})
        spring_rabbit = dict(self.get("spring.rabbitmq", {}) or {})
        if spring_rabbit:
            host = spring_rabbit.get("host")
            port = spring_rabbit.get("port") or 5672
            username = spring_rabbit.get("username") or "guest"
            password = spring_rabbit.get("password") or "guest"
            virtual_host = str(spring_rabbit.get("virtual-host") or spring_rabbit.get("virtualHost") or "/")
            if host and not config.get("url"):
                from urllib.parse import quote

                vhost = "" if virtual_host == "/" else quote(virtual_host.strip("/"), safe="")
                config["url"] = "amqp://%s:%s@%s:%s/%s" % (
                    quote(str(username)),
                    quote(str(password)),
                    host,
                    port,
                    vhost,
                )
            config["enabled"] = config.get("enabled", True)
        return config

    @property
    def kafka(self) -> Dict[str, Any]:
        return dict(self.get("integrations.kafka", {}) or {})

    @property
    def redis(self) -> Dict[str, Any]:
        config = dict(self.get("integrations.redis", {}) or {})
        spring_redis = dict(self.get("spring.redis", {}) or self.get("spring.data.redis", {}) or {})
        if spring_redis:
            host = spring_redis.get("host")
            port = spring_redis.get("port") or 6379
            database = spring_redis.get("database") or 0
            password = spring_redis.get("password")
            if host:
                auth = ":%s@" % password if password else ""
                config["url"] = "redis://%s%s:%s/%s" % (auth, host, port, database)
            config["enabled"] = spring_redis.get("enabled", True)
        return config

    @property
    def telemetry(self) -> Dict[str, Any]:
        return dict(self.get("integrations.telemetry", {}) or {})
