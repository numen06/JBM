import asyncio
import logging
import socket
from contextlib import suppress
from typing import Any, Mapping, Optional

logger = logging.getLogger(__name__)


def local_ip() -> str:
    probe = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        probe.connect(("8.8.8.8", 80))
        return probe.getsockname()[0]
    except OSError:
        return "127.0.0.1"
    finally:
        probe.close()


class NacosRegistrar:
    def __init__(self, service_name: str, port: int, config: Mapping[str, Any]) -> None:
        self.service_name = service_name
        self.port = port
        self.config = dict(config)
        self.enabled = bool(self.config.get("enabled", True))
        self.ip = local_ip()
        self.client: Optional[Any] = None
        self._heartbeat_task: Optional[asyncio.Task[None]] = None

    async def start(self) -> None:
        if not self.enabled:
            logger.info("Nacos discovery disabled")
            return
        server_addr = self.config.get("server-addr")
        if not server_addr:
            logger.warning("Nacos discovery enabled but server-addr is empty")
            return
        try:
            import nacos
        except ImportError:
            logger.warning("nacos-sdk-python is not installed; skip registration")
            return

        try:
            self.client = nacos.NacosClient(
                server_addresses=str(server_addr),
                namespace=str(self.config.get("namespace") or "public"),
            )
            loop = asyncio.get_running_loop()
            await loop.run_in_executor(None, self._register)
            self._heartbeat_task = asyncio.create_task(self._heartbeat_loop())
        except Exception as exc:
            self.client = None
            logger.warning("Nacos registration failed; service will continue locally: %s", exc)

    async def stop(self) -> None:
        if self.client is None:
            return
        if self._heartbeat_task is not None:
            self._heartbeat_task.cancel()
            with suppress(asyncio.CancelledError):
                await self._heartbeat_task
            self._heartbeat_task = None
        loop = asyncio.get_running_loop()
        try:
            await loop.run_in_executor(None, self._deregister)
        except Exception as exc:
            logger.warning("Nacos deregistration failed: %s", exc)

    async def _heartbeat_loop(self) -> None:
        interval = float(self.config.get("heart-beat-interval") or 5)
        loop = asyncio.get_running_loop()
        while True:
            await asyncio.sleep(interval)
            try:
                await loop.run_in_executor(None, self._heartbeat)
            except asyncio.CancelledError:
                raise
            except Exception as exc:
                logger.warning("Nacos heartbeat failed: %s", exc)

    def _register(self) -> None:
        assert self.client is not None
        self.client.add_naming_instance(
            service_name=self.service_name,
            ip=self.ip,
            port=self.port,
            group_name=self._group_name(),
            cluster_name=self._cluster_name(),
            metadata=self._metadata(),
            weight=1.0,
        )
        logger.info("Registered %s:%s to Nacos", self.service_name, self.port)

    def _heartbeat(self) -> None:
        assert self.client is not None
        self.client.send_heartbeat(
            service_name=self.service_name,
            ip=self.ip,
            port=self.port,
            group_name=self._group_name(),
            cluster_name=self._cluster_name(),
            metadata=self._metadata(),
            weight=1.0,
        )

    def _deregister(self) -> None:
        assert self.client is not None
        self.client.remove_naming_instance(
            service_name=self.service_name,
            ip=self.ip,
            port=self.port,
            cluster_name=self._cluster_name(),
            group_name=self._group_name(),
            ephemeral=True,
        )
        logger.info("Deregistered %s:%s from Nacos", self.service_name, self.port)

    def _group_name(self) -> str:
        return str(self.config.get("group") or "DEFAULT_GROUP")

    def _cluster_name(self) -> str:
        return str(self.config.get("cluster-name") or "DEFAULT")

    def _metadata(self) -> dict[str, str]:
        return {"app": self.service_name, "runtime": "python"}


class NacosDiscoveryClient:
    def __init__(self, config: Mapping[str, Any]) -> None:
        self.config = dict(config)
        self.enabled = bool(self.config.get("enabled", True))
        self.client: Optional[Any] = None

    async def start(self) -> None:
        if not self.enabled:
            return
        server_addr = self.config.get("server-addr")
        if not server_addr:
            logger.warning("Nacos discovery enabled but server-addr is empty")
            return
        try:
            import nacos
        except ImportError:
            logger.warning("nacos-sdk-python is not installed; skip discovery client")
            return
        try:
            self.client = nacos.NacosClient(
                server_addresses=str(server_addr),
                namespace=str(self.config.get("namespace") or "public"),
            )
        except Exception as exc:
            logger.warning("Nacos discovery client creation failed: %s", exc)
            self.client = None

    async def stop(self) -> None:
        self.client = None

    async def choose_instance(self, service_name: str) -> Optional[dict[str, Any]]:
        instances = await self.list_instances(service_name)
        return instances[0] if instances else None

    async def list_instances(self, service_name: str) -> list[dict[str, Any]]:
        if self.client is None:
            return []
        loop = asyncio.get_running_loop()
        try:
            return await loop.run_in_executor(None, self._list_instances_sync, service_name)
        except Exception as exc:
            logger.warning("Nacos instance lookup failed for %s: %s", service_name, exc)
            return []

    def _list_instances_sync(self, service_name: str) -> list[dict[str, Any]]:
        assert self.client is not None
        response = self.client.list_naming_instance(
            service_name=service_name,
            group_name=self._group_name(),
        )
        hosts = []
        if isinstance(response, Mapping):
            hosts = list(response.get("hosts") or [])
        healthy_hosts = [
            dict(host)
            for host in hosts
            if host.get("healthy", True) and host.get("enabled", True) and float(host.get("weight") or 1) > 0
        ]
        if healthy_hosts:
            return healthy_hosts
        return [dict(host) for host in hosts]

    def _group_name(self) -> str:
        return str(self.config.get("group") or "DEFAULT_GROUP")
