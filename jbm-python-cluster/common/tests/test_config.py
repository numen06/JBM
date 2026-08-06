import sys
from pathlib import Path
from types import SimpleNamespace

from jbm_cluster_py.common.config import AppConfig, deep_merge


def test_deep_merge_keeps_base_and_overrides_nested_values() -> None:
    merged = deep_merge(
        {"server": {"host": "0.0.0.0", "port": 3312}, "a": 1},
        {"server": {"port": 9999}},
    )

    assert merged["server"]["host"] == "0.0.0.0"
    assert merged["server"]["port"] == 9999
    assert merged["a"] == 1


def test_load_dev_config_from_project_config(monkeypatch) -> None:
    monkeypatch.delenv("JBM_APP", raising=False)
    monkeypatch.setenv(
        "JBM_CONFIG_JSON",
        '{"spring":{"cloud":{"nacos":{"config":{"enabled":false}}}}}',
    )
    monkeypatch.setenv("JBM_NACOS_SERVER_ADDR", "rnacos.test:8848")
    monkeypatch.setenv("JBM_NACOS_NAMESPACE", "jbm-py")
    config_dir = Path(__file__).resolve().parents[1] / "resource"
    config = AppConfig.load(profile="dev", config_dir=config_dir)

    assert config.service_name == "jbm-python-cluster"
    assert config.port == 7777
    assert config.nacos_discovery["namespace"] == "jbm-py"
    assert config.nacos_discovery["server-addr"] == "rnacos.test:8848"
    assert config.nacos_config["server-addr"] == "rnacos.test:8848"

    prod = AppConfig.load(profile="prod", config_dir=config_dir)
    assert prod.profile == "prod"
    assert prod.nacos_discovery["namespace"] == "jbm-py"


def test_load_app_resource_profile_config(monkeypatch) -> None:
    monkeypatch.setenv(
        "JBM_CONFIG_JSON",
        '{"spring":{"cloud":{"nacos":{"config":{"enabled":false}}}}}',
    )
    root = Path(__file__).resolve().parents[2]
    config = AppConfig.load(
        profile="dev",
        app="auth",
        config_dir=root / "common" / "resource",
        resource_dir=root / "auth" / "resource",
    )

    assert config.app == "auth"
    assert config.resource_dir == root / "auth" / "resource"
    assert config.service_name == "jbm-cluster-platform-auth"
    assert config.openapi["title"] == "JBM Python Auth Service"


def test_nacos_data_ids_follow_profile_and_app(monkeypatch) -> None:
    calls = []

    class FakeNacosClient:
        def __init__(self, server_addresses: str, namespace: str) -> None:
            assert server_addresses == "rnacos:8848"
            assert namespace == "jbm-py"

        def get_config(self, data_id: str, group: str) -> str:
            calls.append((data_id, group))
            return "remote:\n  enabled: true\n" if data_id == "common-prod.yml" else ""

    monkeypatch.setitem(sys.modules, "nacos", SimpleNamespace(NacosClient=FakeNacosClient))
    merged = AppConfig._merge_nacos_shared_config(
        {
            "spring": {
                "cloud": {
                    "nacos": {
                        "config": {
                            "enabled": True,
                            "server-addr": "rnacos:8848",
                            "namespace": "jbm-py",
                            "group": "DEFAULT_GROUP",
                            "shared-dataids": "common-{profile}.yml,{app}-{profile}.yml",
                        }
                    }
                }
            }
        },
        "prod",
        "gateway",
    )

    assert calls == [
        ("common-prod.yml", "DEFAULT_GROUP"),
        ("gateway-prod.yml", "DEFAULT_GROUP"),
    ]
    assert merged["remote"]["enabled"] is True
