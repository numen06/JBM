from __future__ import annotations

import re
import shutil
import stat
import zipfile
from collections.abc import Mapping
from pathlib import Path
from typing import Any
from urllib.parse import quote, urlparse

import httpx
from jbm_cluster_py.platform.bigscreen.repository import BigscreenRepository


class BigscreenService:
    def __init__(self, repository: BigscreenRepository, views_dir: str, doc_base_url: str) -> None:
        self.repository = repository
        self.views_dir = Path(views_dir).resolve()
        self.doc_base_url = doc_base_url.rstrip("/")
        self.http = httpx.AsyncClient(timeout=httpx.Timeout(60, connect=10), trust_env=False)

    async def start(self) -> None:
        self.views_dir.mkdir(parents=True, exist_ok=True)
        await self.repository.start()

    async def stop(self) -> None:
        await self.http.aclose()
        await self.repository.stop()

    async def save(self, body: Mapping[str, Any], deploy: bool = True) -> dict[str, Any]:
        data = dict(body)
        parent_id = str(data.get("parentId") or "").strip()
        if parent_id:
            parent = await self.repository.get(parent_id)
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
        saved = await self.repository.save(data)
        if not saved.get("staticParams"):
            saved["staticParams"] = f"id={saved['id']}"
        if not saved.get("viewUrl"):
            saved["viewUrl"] = str(saved["id"])
        saved["viewUrl"] = self._view_key(str(saved["viewUrl"]))
        saved = await self.repository.save(saved)
        if deploy and not await self.is_uploaded(saved):
            await self.upload(saved)
        return saved

    async def upload(self, body: Mapping[str, Any]) -> dict[str, Any]:
        view_id = str(body.get("id") or "").strip()
        if not view_id:
            raise ValueError("ID不能为空")
        view = await self.repository.get(view_id)
        if not view:
            raise ValueError("大屏不存在")
        resource = str(view.get("resourcePath") or "").strip()
        if not resource:
            raise ValueError("没有上传包")
        url = self._resource_url(resource)
        response = await self.http.get(url)
        response.raise_for_status()
        zip_path = self.views_dir / f"{view_id}.zip"
        zip_path.write_bytes(response.content)
        destination = self._view_dir(view)
        if destination.exists():
            shutil.rmtree(destination)
        destination.mkdir(parents=True)
        try:
            self._safe_extract(zip_path, destination)
            if not (destination / "index.html").is_file():
                raise ValueError("不存在index.html首页文件")
        except Exception:
            if destination.exists():
                shutil.rmtree(destination)
            raise
        return view

    async def is_uploaded(self, body: Mapping[str, Any]) -> bool:
        view = await self._resolve(body)
        return (self._view_dir(view) / "index.html").is_file()

    async def clean(self, body: Mapping[str, Any]) -> bool:
        view = await self._resolve(body)
        zip_path = (self.views_dir / f"{view['id']}.zip").resolve()
        view_dir = self._view_dir(view)
        self._assert_child(zip_path)
        self._assert_child(view_dir)
        if zip_path.exists():
            zip_path.unlink()
        if view_dir.exists():
            shutil.rmtree(view_dir)
        return True

    async def delete(self, view_id: str) -> bool:
        if await self.repository.children(view_id):
            raise ValueError("存在子视图不允许删除")
        view = await self.repository.get(view_id)
        if view:
            await self.clean(view)
        return await self.repository.delete(view_id)

    async def load_all(self) -> None:
        for view in (await self.repository.page({}, True))["contents"]:
            if not await self.is_uploaded(view):
                try:
                    await self.upload(view)
                except Exception:
                    continue

    async def _resolve(self, body: Mapping[str, Any]) -> dict[str, Any]:
        view_id = str(body.get("id") or "").strip()
        if not view_id:
            raise ValueError("ID不能为空")
        view = await self.repository.get(view_id)
        if not view:
            raise ValueError("大屏不存在")
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
            for item in package.infolist():
                target = (destination / item.filename).resolve()
                if destination != target and destination not in target.parents:
                    raise ValueError("资源包包含非法路径")
                if stat.S_ISLNK(item.external_attr >> 16):
                    raise ValueError("资源包不允许符号链接")
            package.extractall(destination)

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
