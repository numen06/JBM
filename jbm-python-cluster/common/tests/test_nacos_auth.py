import asyncio
import json
import sys
from types import SimpleNamespace

import pytest

from jbm_cluster_py.common import nacos as nacos_auth
from jbm_cluster_py.integrations.kafka import KafkaClient
from jbm_cluster_py.integrations import nacos as nacos_integration


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


@pytest.mark.asyncio
async def test_nacos_registrar_retries_until_registration_succeeds(monkeypatch) -> None:
    class FakeClient:
        def __init__(self) -> None:
            self.register_calls = 0
            self.remove_calls = 0

        def add_naming_instance(self, **_kwargs) -> None:
            self.register_calls += 1
            if self.register_calls == 1:
                raise RuntimeError("nacos is still initializing")

        def remove_naming_instance(self, **_kwargs) -> None:
            self.remove_calls += 1

    client = FakeClient()
    monkeypatch.setitem(sys.modules, "nacos", SimpleNamespace())
    monkeypatch.setattr(nacos_integration, "create_nacos_client", lambda *_args: client)
    monkeypatch.setattr(nacos_integration, "local_ip", lambda: "127.0.0.1")

    registrar = nacos_integration.NacosRegistrar(
        "test-service",
        8080,
        {
            "server-addr": "rnacos:8848",
            "registration-retry-interval": 0.01,
            "heart-beat-interval": 3600,
        },
    )
    await registrar.start()
    for _ in range(20):
        if registrar._registered:
            break
        await asyncio.sleep(0.01)

    assert registrar._registered is True
    assert client.register_calls == 2

    await registrar.stop()
    assert client.remove_calls == 1
