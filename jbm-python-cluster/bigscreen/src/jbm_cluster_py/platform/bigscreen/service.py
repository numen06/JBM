from __future__ import annotations

import re
import shutil
import stat
import uuid
import zipfile
from collections.abc import Mapping
from pathlib import Path
from typing import Any
from urllib.parse import quote, urlparse

import httpx
from jbm_cluster_py.platform.bigscreen.repository import BigscreenRepository


class BigscreenService:
    def __init__(
        self,
        repository: BigscreenRepository,
        views_dir: str,
        doc_base_url: str,
        *,
        max_package_bytes: int = 100 * 1024 * 1024,
        max_unpacked_bytes: int = 500 * 1024 * 1024,
        max_package_files: int = 5000,
    ) -> None:
        self.repository = repository
        self.views_dir = Path(views_dir).resolve()
        self.doc_base_url = doc_base_url.rstrip("/")
        self.max_package_bytes = max_package_bytes
        self.max_unpacked_bytes = max_unpacked_bytes
        self.max_package_files = max_package_files
        self.http = httpx.AsyncClient(timeout=httpx.Timeout(60, connect=10), trust_env=False)

    async def start(self) -> None:
        self.views_dir.mkdir(parents=True, exist_ok=True)
        await self.repository.start()

    async def stop(self) -> None:
        await self.http.aclose()
        await self.repository.stop()

    async def save(
        self,
        body: Mapping[str, Any],
        deploy: bool = True,
        tenant_id: str | None = None,
        user_id: str | None = None,
    ) -> dict[str, Any]:
        view_id = str(body.get("id") or "").strip()
        current = await self.repository.get(view_id, tenant_id) if view_id else None
        if view_id and tenant_id is not None and current is None:
            raise ValueError("大屏不存在或无权访问")
        data = {**(current or {}), **dict(body)}
        if tenant_id is not None:
            data["tenantId"] = tenant_id
        if user_id and not data.get("createdBy"):
            data["createdBy"] = user_id
        parent_id = str(data.get("parentId") or "").strip()
        if parent_id:
            parent = await self.repository.get(parent_id, tenant_id)
            if not parent:
                raise ValueError("不存在父视图")
            for key in ("viewUrl", "resourcePath", "staticParams", "previewPicture", "configData"):
                data.setdefault(key, parent.get(key))
            data.setdefault("viewName", f"{parent.get('viewName') or '大屏'}_COPY")
        if not str(data.get("resourcePath") or "").strip():
            raise ValueError("没有上传包")
        if not str(data.get("viewName") or "").strip():
            raise ValueError("没有设置大屏名称")
        data["version"] = self._next_version(str(data.get("version") or ""))
        saved = await self.repository.save(data, tenant_id)
        if not saved.get("staticParams"):
            saved["staticParams"] = f"id={saved['id']}"
        if not saved.get("viewUrl"):
            saved["viewUrl"] = str(saved["id"])
        saved["viewUrl"] = self._view_key(str(saved["viewUrl"]))
        saved = await self.repository.save(saved, tenant_id)
        if deploy and not await self.is_uploaded(saved, tenant_id):
            await self.upload(saved, tenant_id)
        return saved

    async def upload(
        self, body: Mapping[str, Any], tenant_id: str | None = None
    ) -> dict[str, Any]:
        view_id = str(body.get("id") or "").strip()
        if not view_id:
            raise ValueError("ID不能为空")
        view = await self.repository.get(view_id, tenant_id)
        if not view:
            raise ValueError("大屏不存在或无权访问")
        resource = str(view.get("resourcePath") or "").strip()
        if not resource:
            raise ValueError("没有上传包")
        temporary = self.views_dir / f".{view_id}-{uuid.uuid4().hex}.upload"
        zip_path = self.views_dir / f"{view_id}.zip"
        try:
            if resource.startswith("local:"):
                source = (self.views_dir / resource.removeprefix("local:")).resolve()
                self._assert_child(source)
                if not source.is_file():
                    raise ValueError("大屏包不存在")
                shutil.copyfile(source, temporary)
            else:
                await self._download(self._resource_url(resource), temporary)
            self._deploy_archive(temporary, view)
            temporary.replace(zip_path)
        finally:
            temporary.unlink(missing_ok=True)
        return view

    async def upload_package(
        self,
        package: Any,
        body: Mapping[str, Any],
        tenant_id: str,
        user_id: str,
    ) -> dict[str, Any]:
        filename = str(getattr(package, "filename", "") or "")
        if not filename.lower().endswith(".zip"):
            raise ValueError("仅支持 ZIP 大屏包")
        view_id = str(body.get("id") or uuid.uuid4().hex).strip()
        existing = await self.repository.get(view_id, tenant_id) if body.get("id") else None
        if body.get("id") and not existing:
            raise ValueError("大屏不存在或无权访问")
        data = {
            **(existing or {}),
            **{key: value for key, value in body.items() if value not in (None, "")},
            "id": view_id,
            "tenantId": tenant_id,
            "createdBy": (existing or {}).get("createdBy") or user_id,
            "viewUrl": str((existing or {}).get("viewUrl") or view_id),
            "resourcePath": f"local:{view_id}.zip",
            "version": self._next_version(str((existing or {}).get("version") or "")),
        }
        if not str(data.get("viewName") or "").strip():
            raise ValueError("没有设置大屏名称")
        data["viewUrl"] = self._view_key(str(data["viewUrl"]))
        data.setdefault("staticParams", f"id={view_id}")
        temporary = self.views_dir / f".{view_id}-{uuid.uuid4().hex}.upload"
        zip_path = self.views_dir / f"{view_id}.zip"
        try:
            await self._store_upload(package, temporary)
            self._deploy_archive(temporary, data)
            temporary.replace(zip_path)
            return await self.repository.save(data, tenant_id)
        finally:
            temporary.unlink(missing_ok=True)

    async def is_uploaded(
        self, body: Mapping[str, Any], tenant_id: str | None = None
    ) -> bool:
        view = await self._resolve(body, tenant_id)
        return self.deployment_status(view)["deployed"]

    def deployment_status(self, view: Mapping[str, Any]) -> dict[str, bool]:
        resource = str(view.get("resourcePath") or "").strip()
        archive = (self.views_dir / f"{view['id']}.zip").resolve()
        self._assert_child(archive)
        return {
            "deployed": (self._view_dir(view) / "index.html").is_file(),
            "packageAvailable": (
                archive.is_file() if resource.startswith("local:") else bool(resource)
            ),
        }

    async def reload(
        self, body: Mapping[str, Any], tenant_id: str | None = None
    ) -> dict[str, Any]:
        view = await self._resolve(body, tenant_id)
        return await self.upload(view, tenant_id)

    async def clean(
        self,
        body: Mapping[str, Any],
        tenant_id: str | None = None,
        *,
        remove_package: bool = False,
    ) -> bool:
        view = await self._resolve(body, tenant_id)
        zip_path = (self.views_dir / f"{view['id']}.zip").resolve()
        view_dir = self._view_dir(view)
        self._assert_child(zip_path)
        self._assert_child(view_dir)
        if remove_package and zip_path.exists():
            zip_path.unlink()
        if view_dir.exists():
            shutil.rmtree(view_dir)
        return True

    async def delete(self, view_id: str, tenant_id: str | None = None) -> bool:
        if await self.repository.children(view_id, tenant_id):
            raise ValueError("存在子视图不允许删除")
        view = await self.repository.get(view_id, tenant_id)
        if view:
            await self.clean(view, tenant_id, remove_package=True)
        return await self.repository.delete(view_id, tenant_id)

    async def load_all(self) -> None:
        for view in (await self.repository.page({}, True))["contents"]:
            if not await self.is_uploaded(view):
                try:
                    await self.upload(view)
                except Exception:
                    continue

    async def _resolve(
        self, body: Mapping[str, Any], tenant_id: str | None = None
    ) -> dict[str, Any]:
        view_id = str(body.get("id") or "").strip()
        if not view_id:
            raise ValueError("ID不能为空")
        view = await self.repository.get(view_id, tenant_id)
        if not view:
            raise ValueError("大屏不存在或无权访问")
        return view

    def _view_dir(self, view: Mapping[str, Any]) -> Path:
        result = (self.views_dir / self._view_key(str(view.get("viewUrl") or view["id"]))).resolve()
        self._assert_child(result)
        return result

    def _assert_child(self, path: Path) -> None:
        if path != self.views_dir and self.views_dir not in path.parents:
            raise ValueError("非法大屏路径")

    @staticmethod
    def _view_key(value: str) -> str:
        key = value.strip().strip("/")
        if not key or not re.fullmatch(r"[A-Za-z0-9._-]+", key):
            raise ValueError("不是合法地址:/xxxx")
        return key

    def _safe_extract(self, archive: Path, destination: Path) -> None:
        with zipfile.ZipFile(archive) as package:
            items = package.infolist()
            if len(items) > self.max_package_files:
                raise ValueError("大屏包文件数量超过限制")
            unpacked = sum(item.file_size for item in items)
            if unpacked > self.max_unpacked_bytes:
                raise ValueError("大屏包解压后体积超过限制")
            for item in items:
                if item.flag_bits & 0x1:
                    raise ValueError("大屏包不允许加密文件")
                target = (destination / item.filename).resolve()
                if destination != target and destination not in target.parents:
                    raise ValueError("资源包包含非法路径")
                if stat.S_ISLNK(item.external_attr >> 16):
                    raise ValueError("资源包不允许符号链接")
            package.extractall(destination)

    async def _store_upload(self, package: Any, target: Path) -> None:
        total = 0
        with target.open("wb") as output:
            while chunk := await package.read(1024 * 1024):
                total += len(chunk)
                if total > self.max_package_bytes:
                    raise ValueError("大屏包体积超过限制")
                output.write(chunk)
        if total == 0 or not zipfile.is_zipfile(target):
            raise ValueError("上传文件不是有效的 ZIP 包")

    async def _download(self, url: str, target: Path) -> None:
        total = 0
        async with self.http.stream("GET", url) as response:
            response.raise_for_status()
            length = int(response.headers.get("content-length") or 0)
            if length > self.max_package_bytes:
                raise ValueError("大屏包体积超过限制")
            with target.open("wb") as output:
                async for chunk in response.aiter_bytes(1024 * 1024):
                    total += len(chunk)
                    if total > self.max_package_bytes:
                        raise ValueError("大屏包体积超过限制")
                    output.write(chunk)
        if total == 0 or not zipfile.is_zipfile(target):
            raise ValueError("大屏资源不是有效的 ZIP 包")

    def _deploy_archive(self, archive: Path, view: Mapping[str, Any]) -> None:
        destination = self._view_dir(view)
        token = uuid.uuid4().hex
        staging = self.views_dir / f".deploy-{token}"
        backup = self.views_dir / f".backup-{token}"
        staging.mkdir()
        moved_existing = False
        try:
            self._safe_extract(archive, staging)
            if not (staging / "index.html").is_file():
                raise ValueError("大屏包根目录不存在 index.html")
            if destination.exists():
                destination.rename(backup)
                moved_existing = True
            staging.rename(destination)
            if backup.exists():
                shutil.rmtree(backup)
        except Exception:
            if moved_existing and backup.exists() and not destination.exists():
                backup.rename(destination)
            raise
        finally:
            if staging.exists():
                shutil.rmtree(staging)
            if backup.exists():
                shutil.rmtree(backup)

    def _resource_url(self, resource: str) -> str:
        if urlparse(resource).scheme in {"http", "https"}:
            return resource
        return f"{self.doc_base_url}/download/{quote(resource.lstrip('/'), safe='/')}"

    @staticmethod
    def _next_version(value: str) -> str:
        try:
            major, minor, patch = (int(part) for part in value.split(".", 2))
            return f"{major}.{minor}.{patch + 1}"
        except (TypeError, ValueError):
            return "1.0.0"
