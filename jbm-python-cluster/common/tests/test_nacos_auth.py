import json
from types import SimpleNamespace

import pytest

from jbm_cluster_py.common import nacos as nacos_auth
from jbm_cluster_py.integrations.kafka import KafkaClient


def test_nacos_token_is_added_and_reused(monkeypatch) -> None:
    class FakeClient:
        def __init__(self, server_addresses: str, namespace: str) -> None:
            self.server_addresses = server_addresses
            self.namespace = namespace

    class FakeResponse:
        def __enter__(self):
            return self

        def __exit__(self, *_args) -> None:
            pass

        def read(self) -> bytes:
            return json.dumps({"accessToken": "token-1", "tokenTtl": 3600}).encode()

    calls = []

    class FakeOpener:
        def open(self, request, timeout: int):
            calls.append((request.full_url, request.data, timeout))
            return FakeResponse()

    monkeypatch.setattr(nacos_auth, "build_opener", lambda *_args: FakeOpener())
    client = nacos_auth.create_nacos_client(
        SimpleNamespace(NacosClient=FakeClient),
        "rnacos:8848",
        "jbm-py",
        {"username": "jbm", "password": "secret"},
    )

    first, second = {}, {}
    client._inject_auth_info({}, first, None)
    client._inject_auth_info({}, second, None)

    assert first == second == {"accessToken": "token-1"}
    assert len(calls) == 1
    assert calls[0][0] == "http://rnacos:8848/nacos/v1/auth/login"


def test_kafka_sasl_options_require_and_include_nacos_credentials() -> None:
    with pytest.raises(ValueError, match="username and password"):
        KafkaClient({"enabled": True, "security-protocol": "SASL_PLAINTEXT"})

    client = KafkaClient(
        {
            "enabled": True,
            "security-protocol": "SASL_PLAINTEXT",
            "sasl-mechanism": "PLAIN",
            "username": "jbm",
            "password": "secret",
        }
    )
    assert client.client_options == {
        "security_protocol": "SASL_PLAINTEXT",
        "sasl_mechanism": "PLAIN",
        "sasl_plain_username": "jbm",
        "sasl_plain_password": "secret",
    }
