import copy
import os
from pathlib import Path
from typing import Any, Dict, Iterable, List, Mapping, Optional

import yaml


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


def load_yaml_file(path: Path) -> Dict[str, Any]:
    if not path.exists():
        return {}
    with path.open("r", encoding="utf-8") as handle:
        return yaml.safe_load(handle) or {}


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
        return cls(merged, selected_profile, base_dir, selected_app, app_resource_dir, resource_dirs)

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
        return str(self.get("spring.application.name", "jbm-cluster-platform-logs"))

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
        return dict(self.get("integrations.database", {}) or {})

    @property
    def minio(self) -> Dict[str, Any]:
        config = dict(self.get("integrations.minio", {}) or {})
        spring_minio = dict(self.get("spring.minio", {}) or {})
        if spring_minio:
            mapped = {
                "endpoint-url": spring_minio.get("endpoint-url") or spring_minio.get("endpointUrl") or spring_minio.get("url"),
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
        return dict(self.get("integrations.openobserve", {}) or {})

    @property
    def rabbitmq(self) -> Dict[str, Any]:
        return dict(self.get("integrations.rabbitmq", {}) or {})

    @property
    def redis(self) -> Dict[str, Any]:
        return dict(self.get("integrations.redis", {}) or {})

    @property
    def telemetry(self) -> Dict[str, Any]:
        return dict(self.get("integrations.telemetry", {}) or {})
