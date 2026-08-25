import asyncio
import json
import logging
import socket
from contextlib import suppress
from typing import Any, Mapping, Optional
from urllib.parse import urlencode
from urllib.request import ProxyHandler, build_opener

from jbm_cluster_py.common.nacos import create_nacos_client

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
        self._registered = False
        self._registration_task: Optional[asyncio.Task[None]] = None
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
            self.client = create_nacos_client(
                nacos,
                str(server_addr),
                str(self.config.get("namespace") or "public"),
                self.config,
            )
        except Exception as exc:
            logger.warning("Nacos client creation failed; service will continue locally: %s", exc)
            return
        self._registration_task = asyncio.create_task(self._registration_loop())

    async def stop(self) -> None:
        if self._registration_task is not None:
            self._registration_task.cancel()
            with suppress(asyncio.CancelledError):
                await self._registration_task
            self._registration_task = None
        if self._heartbeat_task is not None:
            self._heartbeat_task.cancel()
            with suppress(asyncio.CancelledError):
                await self._heartbeat_task
            self._heartbeat_task = None
        if self.client is None or not self._registered:
            self.client = None
            return
        loop = asyncio.get_running_loop()
        try:
            await loop.run_in_executor(None, self._deregister)
        except Exception as exc:
            logger.warning("Nacos deregistration failed: %s", exc)
        finally:
            self._registered = False
            self.client = None

    async def _registration_loop(self) -> None:
        interval = float(self.config.get("registration-retry-interval") or 2)
        loop = asyncio.get_running_loop()
        while not self._registered:
            try:
                await loop.run_in_executor(None, self._register)
                self._registered = True
                self._heartbeat_task = asyncio.create_task(self._heartbeat_loop())
            except asyncio.CancelledError:
                raise
            except Exception as exc:
                logger.warning("Nacos registration failed; retrying in %ss: %s", interval, exc)
                await asyncio.sleep(interval)

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
        configured = self.config.get("metadata") or {}
        metadata = {
            str(key): str(value)
            for key, value in dict(configured).items()
            if value is not None
        }
        return {"app": self.service_name, "runtime": "python", **metadata}


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
            self.client = create_nacos_client(
                nacos,
                str(server_addr),
                str(self.config.get("namespace") or "public"),
                self.config,
            )
        except Exception as exc:
            logger.warning("Nacos discovery client creation failed: %s", exc)
            self.client = None

    async def stop(self) -> None:
        self.client = None

    async def choose_instance(self, service_name: str) -> Optional[dict[str, Any]]:
        instances = await self.list_instances(service_name)
        return instances[0] if instances else None

    async def list_services(self) -> list[str]:
        if self.client is None:
            return []
        loop = asyncio.get_running_loop()
        try:
            return await loop.run_in_executor(None, self._list_services_sync)
        except Exception as exc:
            logger.warning("Nacos service lookup failed: %s", exc)
            return []

    def _list_services_sync(self) -> list[str]:
        server = str(self.config.get("server-addr") or "").split(",", 1)[0].strip().rstrip("/")
        if not server:
            return []
        if not server.startswith(("http://", "https://")):
            server = "http://" + server
        request_params = {
            "pageNo": 1,
            "pageSize": 1000,
            "groupName": self._group_name(),
            "namespaceId": str(self.config.get("namespace") or "public"),
        }
        assert self.client is not None
        self.client._inject_auth_info({}, request_params, None, "naming")
        params = urlencode(request_params)
        with build_opener(ProxyHandler({})).open(
            f"{server}/nacos/v1/ns/service/list?{params}", timeout=5
        ) as response:
            body = json.loads(response.read().decode("utf-8"))
        return [str(item) for item in body.get("doms") or []]

    async def list_instances(self, service_name: str) -> list[dict[str, Any]]:
        if self.client is None:
            return []
        loop = asyncio.get_running_loop()
        try:
            return await loop.run_in_executor(None, self._list_instances_sync, service_name)
        except Exception as exc:
            logger.warning("Nacos instance lookup failed for %s: %s", service_name, exc)
            return []

    async def get_config(self, data_id: str, group: Optional[str] = None) -> str:
        if self.client is None:
            raise RuntimeError("Nacos client is not available")
        target_group = group or str(self.config.get("config-group") or self._group_name())
        value = await asyncio.to_thread(self.client.get_config, data_id, target_group)
        return str(value or "")

    async def publish_config(
        self,
        data_id: str,
        content: str,
        group: Optional[str] = None,
    ) -> None:
        if self.client is None:
            raise RuntimeError("Nacos client is not available")
        target_group = group or str(self.config.get("config-group") or self._group_name())
        published = await asyncio.to_thread(
            self.client.publish_config,
            data_id,
            target_group,
            content,
            config_type="yaml",
        )
        if not published:
            raise RuntimeError(f"Nacos rejected config publication: {data_id}")

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
