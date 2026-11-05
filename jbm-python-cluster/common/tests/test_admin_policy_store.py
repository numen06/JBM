import io
import json
import pytest
from fastapi import HTTPException
from jbm_cluster_py.common.admin_security import (
    AdminAccessPolicy,
    AdminPolicyStore,
    DEFAULT_ADMIN_NETWORKS,
)

ROOT = {"userId": 1, "roles": ["super_admin"]}


class Config:
    def get(self, *args):
        return {}


class Nacos:
    namespace = "test"

    def __init__(self):
        self.raw = None

    def get_config(self, *args, **kwargs):
        return self.raw

    def _do_sync_req(self, path, headers, query, data, timeout, method):
        self.raw = data["content"]
        return io.BytesIO(b"true")


def test_lan_defaults_and_self_lock_prevention_and_revision():
    store = AdminPolicyStore(Config())
    store.client = Nacos()
    original = AdminAccessPolicy()
    assert original.evaluate(ROOT, "192.168.3.8") and not original.evaluate(ROOT, "203.0.113.9")
    for networks in [["10.0.0.0/8"], ["invalid"], ["0.0.0.0/0"], []]:
        with pytest.raises(HTTPException) as exc:
            store.save({"mode": "enforce", "allowedNetworks": networks}, ROOT, "192.168.3.8")
        assert exc.value.status_code == 400
    assert store.client.raw is None
    store.save(
        {"mode": "enforce", "allowedNetworks": list(DEFAULT_ADMIN_NETWORKS)}, ROOT, "192.168.3.8"
    )
    assert original.mode == "enforce" and original.revision
    with pytest.raises(HTTPException) as exc:
        store.save(
            {
                "mode": "monitor",
                "allowedNetworks": list(DEFAULT_ADMIN_NETWORKS),
                "revision": "stale",
            },
            ROOT,
            "192.168.3.8",
        )
    assert exc.value.status_code == 409
    store.save(
        {
            "mode": "monitor",
            "allowedNetworks": list(DEFAULT_ADMIN_NETWORKS),
            "revision": original.revision,
        },
        ROOT,
        "192.168.3.8",
    )
    assert original.mode == "monitor"
    with pytest.raises(ValueError):
        store.apply(json.dumps({"mode": "enforce", "allowed-networks": []}))
    assert original.mode == "monitor"
