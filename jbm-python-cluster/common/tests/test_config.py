from pathlib import Path

from jbm_cluster_py.common.config import AppConfig, deep_merge


def test_deep_merge_keeps_base_and_overrides_nested_values() -> None:
    merged = deep_merge(
        {"server": {"host": "0.0.0.0", "port": 3312}, "a": 1},
        {"server": {"port": 9999}},
    )

    assert merged["server"]["host"] == "0.0.0.0"
    assert merged["server"]["port"] == 9999
    assert merged["a"] == 1


def test_load_jaja7_config_from_project_config() -> None:
    config_dir = Path(__file__).resolve().parents[1] / "resource"
    config = AppConfig.load(profile="jaja7", config_dir=config_dir)

    assert config.service_name == "jbm-cluster-platform-logs"
    assert config.port == 3312
    assert config.nacos_discovery["namespace"] == "jbm7"


def test_load_app_resource_profile_config() -> None:
    root = Path(__file__).resolve().parents[2]
    config = AppConfig.load(
        profile="jaja7",
        app="logs",
        config_dir=root / "common" / "resource",
        resource_dir=root / "logs" / "resource",
    )

    assert config.app == "logs"
    assert config.resource_dir == root / "logs" / "resource"
    assert config.service_name == "jbm-cluster-platform-logs"
    assert config.openapi["title"] == "JBM Python Logs Service"
