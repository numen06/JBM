import asyncio
from types import SimpleNamespace

from jbm_cluster_py.platform.auth.service import TokenCache


def test_session_scan_batches_shared_redis_and_preserves_expired_key_handling():
    class Redis:
        async def scan_iter(self, match, *, count):
            assert match == "jbm:auth:access:*"
            assert count == 1000
            for key in ("live", "expired", "other-live"):
                yield key

        async def get(self, key):
            return {"live": b'{"tokenId":"one","zero":0}',
                    "other-live": b'{"tokenId":"two","enabled":false}'}.get(key)

    cache = TokenCache(SimpleNamespace(client=Redis()))
    assert asyncio.run(cache.list_json("access:")) == [
        {"tokenId": "one", "zero": 0}, {"tokenId": "two", "enabled": False},
    ]
