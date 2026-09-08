from __future__ import annotations

import asyncio
import logging
import re
import shutil
import stat
import time
import uuid
import zipfile
from collections.abc import Mapping
from contextlib import contextmanager
from datetime import UTC, datetime
from pathlib import Path
from typing import Any
from urllib.parse import quote, urlparse

import httpx
from jbm_cluster_py.platform.bigscreen.history import (
    PackageHistory, byte_size, checksum, extension, keep_versions, storage_key,
)
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
        self.history = PackageHistory(self.views_dir)
        # ponytail: one writer per process (the shipped Uvicorn setup); use a shared
        # lock before running multiple writers against the same resource volume.
        self._mutation_lock = asyncio.Lock()
        self.http = httpx.AsyncClient(timeout=httpx.Timeout(60, connect=10), trust_env=False)

    async def start(self) -> None:
        self.views_dir.mkdir(parents=True, exist_ok=True)
        self.history.start()
        await self.repository.start()
        self._cleanup_temporary()

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
        async with self._mutation_lock:
            return await self._save(body, deploy, tenant_id, user_id)

    async def _save(self, body, deploy, tenant_id, user_id) -> dict[str, Any]:
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
        data["id"] = storage_key(view_id or uuid.uuid4().hex)
        data["version"] = self._next_version(str(data.get("version") or ""))
        data["staticParams"] = data.get("staticParams") or f"id={data['id']}"
        data["viewUrl"] = self._view_key(str(data.get("viewUrl") or data["id"]))
        if deploy and not self.deployment_status(data)["deployed"]:
            temporary = self.history.work / f"{uuid.uuid4().hex}.upload"
            try:
                await self._stage_resource(data, temporary)
                return await self._publish(temporary, data, current, tenant_id)
            finally:
                temporary.unlink(missing_ok=True)
        return await self.repository.save(data, tenant_id)

    async def upload(
        self, body: Mapping[str, Any], tenant_id: str | None = None
    ) -> dict[str, Any]:
        async with self._mutation_lock:
            return await self._upload(body, tenant_id)

    async def _upload(self, body, tenant_id) -> dict[str, Any]:
        view_id = str(body.get("id") or "").strip()
        if not view_id:
            raise ValueError("ID不能为空")
        view = await self.repository.get(view_id, tenant_id)
        if not view:
            raise ValueError("大屏不存在或无权访问")
        temporary = self.history.work / f"{uuid.uuid4().hex}.upload"
        zip_path = self._archive_path(view)
        try:
            await self._stage_resource(view, temporary)
            data = dict(view)
            if zip_path.is_file() and checksum(zip_path) != checksum(temporary):
                data["version"] = self._next_version(str(view.get("version") or ""))
            return await self._publish(temporary, data, view, tenant_id)
        finally:
            temporary.unlink(missing_ok=True)

    async def _stage_resource(self, view, temporary) -> None:
        resource = str(view.get("resourcePath") or "").strip()
        if not resource:
            raise ValueError("没有上传包")
        if resource.startswith("local:"):
            source = (self.views_dir / resource.removeprefix("local:")).resolve()
            self._assert_child(source)
            if not source.is_file():
                raise ValueError("大屏包不存在")
            if source.stat().st_size > self.max_package_bytes:
                raise ValueError("大屏包体积超过限制")
            shutil.copyfile(source, temporary)
        else:
            await self._download(self._resource_url(resource), temporary)

    async def upload_package(
        self,
        package: Any,
        body: Mapping[str, Any],
        tenant_id: str,
        user_id: str,
    ) -> dict[str, Any]:
        async with self._mutation_lock:
            return await self._upload_package(package, body, tenant_id, user_id)

    async def _upload_package(self, package, body, tenant_id, user_id) -> dict[str, Any]:
        filename = str(getattr(package, "filename", "") or "")
        if not filename.lower().endswith(".zip"):
            raise ValueError("仅支持 ZIP 大屏包")
        view_id = storage_key(body.get("id") or uuid.uuid4().hex)
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
        temporary = self.history.work / f"{uuid.uuid4().hex}.upload"
        try:
            await self._store_upload(package, temporary)
            return await self._publish(temporary, data, existing, tenant_id)
        finally:
            temporary.unlink(missing_ok=True)

    async def is_uploaded(
        self, body: Mapping[str, Any], tenant_id: str | None = None
    ) -> bool:
        view = await self._resolve(body, tenant_id)
        return self.deployment_status(view)["deployed"]

    def deployment_status(self, view: Mapping[str, Any]) -> dict[str, bool]:
        resource = str(view.get("resourcePath") or "").strip()
        archive = self._archive_path(view)
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
        async with self._mutation_lock:
            view = await self._resolve(body, tenant_id)
            await self._assert_exclusive(view)
            self._clean(view, remove_package=remove_package)
            return True

    def _clean(self, view, *, remove_package=False) -> None:
        zip_path = self._archive_path(view)
        view_dir = self._view_dir(view)
        self._assert_child(zip_path)
        self._assert_child(view_dir)
        if remove_package and zip_path.exists():
            zip_path.unlink()
        if view_dir.exists():
            shutil.rmtree(view_dir)

    async def delete(self, view_id: str, tenant_id: str | None = None) -> bool:
        async with self._mutation_lock:
            if await self.repository.children(view_id, tenant_id):
                raise ValueError("存在子视图不允许删除")
            view = await self._resolve({"id": view_id}, tenant_id)
            protected = await self._protected_paths(view)
            result = await self.repository.delete(view_id, tenant_id)
            if self._view_dir(view) not in protected and self._archive_path(view) not in protected:
                self._clean(view, remove_package=True)
            self.history.remove(view)
            return result

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
        result = self.views_dir / self._view_key(str(view.get("viewUrl") or view["id"]))
        if result.is_symlink():
            raise ValueError("大屏资源不允许符号链接")
        result = result.resolve()
        self._assert_child(result)
        return result

    def _assert_child(self, path: Path) -> None:
        if path == self.views_dir or self.views_dir not in path.parents:
            raise ValueError("非法大屏路径")

    @staticmethod
    def _view_key(value: str) -> str:
        return storage_key(value.strip().strip("/"))

    def _archive_path(self, view) -> Path:
        path = self.views_dir / f"{storage_key(view['id'])}.zip"
        if path.is_symlink():
            raise ValueError("大屏资源不允许符号链接")
        self._assert_child(path.resolve())
        return path

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

    async def _protected_paths(self, view) -> set[Path]:
        protected = set()
        for other in (await self.repository.page({}, True))["contents"]:
            if str(other["id"]) == str(view["id"]):
                continue
            protected.add(self._view_dir(other))
            protected.add(self._archive_path(other))
            resource = str(other.get("resourcePath") or "")
            if resource.startswith("local:"):
                protected.add((self.views_dir / resource.removeprefix("local:")).resolve())
        return protected

    async def _assert_exclusive(self, view) -> None:
        protected = await self._protected_paths(view)
        if self._view_dir(view) in protected or self._archive_path(view) in protected:
            raise ValueError("资源仍被其他大屏引用，请先为大屏配置独立资源地址")

    async def _publish(self, temporary, data, existing, tenant_id):
        await self._assert_exclusive(data)
        remembered = None
        digest = checksum(temporary)
        try:
            with self._deploy_archive(temporary, data) as previous_zip:
                if existing and previous_zip.is_file() and checksum(previous_zip) != digest:
                    remembered = self.history.remember(existing, previous_zip)
                saved = await self.repository.save(data, tenant_id)
        except BaseException:
            if remembered:
                try:
                    self.history.forget(existing, remembered)
                except OSError:
                    logging.getLogger(__name__).exception("发布失败后重复备份清理暂缓")
            raise
        try:
            self.history.prune(saved, digest)
            self._cleanup_temporary()
        except OSError:
            logging.getLogger(__name__).exception("大屏已发布，旧资源清理暂缓")
            saved["retentionWarning"] = "大屏已发布，旧资源清理暂缓，请在版本与空间中重试"
        return saved

    @contextmanager
    def _deploy_archive(self, archive: Path, view: Mapping[str, Any]):
        """Keep the previous ZIP and directory until metadata commits successfully."""
        destination = self._view_dir(view)
        zip_path = self._archive_path(view)
        transaction = self.history.work / uuid.uuid4().hex
        transaction.mkdir()
        staging = transaction / "new"
        backup = transaction / "previous"
        previous_zip = transaction / "previous.zip"
        staging.mkdir()
        moved_existing = False
        moved_zip = False
        installed_directory = False
        installed_zip = False
        safe_to_remove = False
        try:
            self._safe_extract(archive, staging)
            if not (staging / "index.html").is_file():
                raise ValueError("大屏包根目录不存在 index.html")
            if destination.exists():
                destination.rename(backup)
                moved_existing = True
            if zip_path.exists():
                zip_path.rename(previous_zip)
                moved_zip = True
            staging.rename(destination)
            installed_directory = True
            archive.replace(zip_path)
            installed_zip = True
            yield previous_zip
            safe_to_remove = True
        except BaseException:
            try:
                if installed_directory:
                    shutil.rmtree(destination)
                if installed_zip:
                    zip_path.unlink()
                if moved_existing:
                    backup.rename(destination)
                if moved_zip:
                    previous_zip.rename(zip_path)
                safe_to_remove = True
            except OSError:
                # Keep rescue files if restoring the prior filesystem state failed.
                logging.getLogger(__name__).exception("恢复大屏失败，保留私有工作目录供恢复")
            raise
        finally:
            if safe_to_remove:
                try:
                    shutil.rmtree(transaction)
                except OSError:
                    logging.getLogger(__name__).exception("大屏工作目录清理暂缓")

    def _legacy_backups(self, view, protected: set[Path]) -> list[Path]:
        archive_pattern = re.compile(re.escape(storage_key(view["id"]))
            + r"\.zip\.bak-(?:\d{12,14}|\d+\.\d+\.\d+(?:-[A-Za-z0-9-]+)?)$")
        directory_pattern = re.compile(re.escape(self._view_key(str(view.get("viewUrl") or view["id"])))
            + r"\.bak-\d{12,14}$")
        return sorted((path for path in self.views_dir.iterdir()
                       if not path.is_symlink() and path.resolve() not in protected
                       and ((path.is_file() and archive_pattern.fullmatch(path.name))
                            or (path.is_dir() and directory_pattern.fullmatch(path.name)))),
                      key=lambda path: path.stat().st_mtime, reverse=True)

    async def storage_status(self, body, tenant_id):
        async with self._mutation_lock:
            view = await self._resolve(body, tenant_id)
            return await self._storage_status(view)

    async def _storage_status(self, view):
        history = self.history.entries(view)
        legacy = self._legacy_backups(view, await self._protected_paths(view))
        current_bytes = byte_size(self._archive_path(view)) + byte_size(self._view_dir(view))
        history_bytes = byte_size(self.history.folder(view))
        legacy_bytes = sum(byte_size(path) for path in legacy)
        return {"id": view["id"], "currentVersion": view.get("version"),
                "keepVersions": keep_versions(view), "maxKeepVersions": 5,
                "currentBytes": current_bytes, "historyBytes": history_bytes,
                "legacyBytes": legacy_bytes, "totalBytes": current_bytes + history_bytes + legacy_bytes,
                "history": [{key: row.get(key) for key in ("id", "version", "createdAt", "sizeBytes")}
                            for row in history],
                "legacyBackups": [{"name": path.name, "sizeBytes": byte_size(path)} for path in legacy]}

    async def set_retention(self, body, tenant_id):
        count = body.get("keepVersions")
        if type(count) is not int or not 1 <= count <= 5:
            raise ValueError("保留版本数必须为1至5的整数（包含当前版本）")
        async with self._mutation_lock:
            view = await self._resolve(body, tenant_id)
            archive = self._archive_path(view)
            if archive.is_file():
                self._validate_backup(archive)
            extra = extension(view)
            extra["resourceRetention"] = {"keepVersions": count}
            view = await self.repository.save({**view, "extendData": extra}, tenant_id)
            # No current package: preserve all remaining recovery options.
            freed = self.history.prune(view, checksum(archive)) if archive.is_file() else 0
            return {**await self._storage_status(view), "releasedBytes": freed}

    async def prune_resources(self, body, tenant_id):
        async with self._mutation_lock:
            view = await self._resolve(body, tenant_id)
            await self._assert_exclusive(view)
            archive = self._archive_path(view)
            if not archive.is_file():
                raise ValueError("当前资源包不存在，已保留全部备份，请先恢复当前版本")
            # Validate the recovery package before discarding any legacy fallback.
            self._validate_backup(archive)
            digest = checksum(archive)
            before_bytes = (await self._storage_status(view))["totalBytes"]
            if body.get("includeLegacy") is True:
                legacy = self._legacy_backups(view, await self._protected_paths(view))
                for path in legacy:
                    if path.is_file() and keep_versions(view) > 1 and checksum(path) != digest:
                        self._validate_backup(path)
                        version = path.name.split(".zip.bak-", 1)[1]
                        match = re.match(r"\d+\.\d+\.\d+", version)
                        self.history.remember(view, path,
                            created_at=datetime.fromtimestamp(path.stat().st_mtime, UTC).isoformat(),
                            version=match[0] if match else f"历史备份-{version}")
                # All archives copied successfully before deleting any originals.
                for path in legacy:
                    if path.is_dir():
                        shutil.rmtree(path)
                    else:
                        path.unlink()
            self.history.prune(view, digest)
            temporary_bytes = self._cleanup_temporary()
            status = await self._storage_status(view)
            return {**status, "releasedBytes": max(0, before_bytes - status["totalBytes"]) + temporary_bytes}

    def _validate_backup(self, archive: Path) -> None:
        try:
            self._check_backup(archive)
        except (zipfile.BadZipFile, RuntimeError, NotImplementedError) as error:
            raise ValueError("备份包校验失败，未清理旧资源") from error

    def _check_backup(self, archive: Path) -> None:
        if archive.stat().st_size > self.max_package_bytes:
            raise ValueError("备份包体积超过限制，未清理旧资源")
        with zipfile.ZipFile(archive) as package:
            files = package.infolist()
            if (len(files) > self.max_package_files
                    or sum(item.file_size for item in files) > self.max_unpacked_bytes
                    or "index.html" not in package.namelist()
                    or any(item.flag_bits & 1 or stat.S_ISLNK(item.external_attr >> 16) for item in files)
                    or package.testzip() is not None):
                raise ValueError("备份包校验失败，未清理旧资源")
            base = self.history.work.resolve()
            if any(base != (base / item.filename).resolve()
                   and base not in (base / item.filename).resolve().parents for item in files):
                raise ValueError("备份包包含非法路径，未清理旧资源")

    async def rollback(self, body, tenant_id):
        async with self._mutation_lock:
            view = await self._resolve(body, tenant_id)
            revision = str(body.get("revisionId") or "")
            entry = next((item for item in self.history.entries(view) if item["id"] == revision), None)
            if not entry:
                raise ValueError("历史版本不存在或已淘汰")
            source = self.history.archive(view, revision)
            if checksum(source) != entry["sha256"]:
                raise ValueError("历史资源校验失败，当前版本未改变")
            temporary = self.history.work / f"{uuid.uuid4().hex}.upload"
            # Keep current scope, name and retention settings; roll back package content.
            data = {**view, "version": self._next_version(str(view.get("version") or "")),
                    "resourcePath": f"local:{view['id']}.zip"}
            try:
                shutil.copyfile(source, temporary)
                return await self._publish(temporary, data, view, tenant_id)
            finally:
                temporary.unlink(missing_ok=True)

    def _cleanup_temporary(self) -> int:
        released = 0
        cutoff = time.time() - 24 * 3600
        for folder in (self.views_dir, self.history.work):
            for path in folder.iterdir():
                recognized = (re.fullmatch(r"[a-f0-9]{32}\.upload", path.name)
                    if folder == self.history.work else
                    re.fullmatch(r"(?:\.[A-Za-z0-9._-]+-[a-f0-9]{32}\.upload|\.deploy-[a-f0-9]{32})", path.name))
                if recognized and not path.is_symlink() and path.stat().st_mtime < cutoff:
                    released += byte_size(path)
                    if path.is_dir():
                        shutil.rmtree(path)
                    else:
                        path.unlink()
        return released

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
