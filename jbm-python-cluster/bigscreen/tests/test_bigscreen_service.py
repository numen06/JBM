from __future__ import annotations

import io
import asyncio
import os
import time
import zipfile

import pytest
from jbm_cluster_py.platform.bigscreen.repository import BigscreenRepository
from jbm_cluster_py.platform.bigscreen.service import BigscreenService
from sqlalchemy.pool import NullPool
from jbm_cluster_py.platform.bigscreen.history import checksum


class Upload:
    def __init__(self, data: bytes, filename: str = "screen.zip") -> None:
        self.filename = filename
        self._stream = io.BytesIO(data)

    async def read(self, size: int = -1) -> bytes:
        return self._stream.read(size)


def package_bytes(label: str, filename: str = "index.html") -> bytes:
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as archive:
        archive.writestr(filename, label)
    return buffer.getvalue()


def database_config(tmp_path) -> dict[str, str]:
    return {"url": f"sqlite+aiosqlite:///{(tmp_path / 'bigscreen.db').as_posix()}"}


@pytest.mark.asyncio
async def test_bigscreen_metadata_inheritance_and_delete_guard(tmp_path) -> None:
    repository = BigscreenRepository(database_config(tmp_path))
    assert isinstance(repository.engine.pool, NullPool)
    service = BigscreenService(repository, str(tmp_path / "views"), "http://doc.invalid")
    await service.start()
    try:
        parent = await service.save(
            {"viewName": "主视图", "viewUrl": "main", "resourcePath": "main.zip"},
            deploy=False,
        )
        child = await service.save(
            {"viewName": "子视图", "parentId": parent["id"]},
            deploy=False,
        )
        assert child["resourcePath"] == "main.zip"
        assert child["viewUrl"] == "main"
        with pytest.raises(ValueError, match="存在子视图"):
            await service.delete(parent["id"])
        assert (await repository.page({}, True))["total"] == 2
    finally:
        await service.stop()


@pytest.mark.asyncio
async def test_bigscreen_zip_extraction_blocks_path_traversal(tmp_path) -> None:
    repository = BigscreenRepository(database_config(tmp_path))
    service = BigscreenService(repository, str(tmp_path / "views"), "http://doc.invalid")
    await service.start()
    try:
        valid_zip = tmp_path / "valid.zip"
        with zipfile.ZipFile(valid_zip, "w") as archive:
            archive.writestr("index.html", "<html>ok</html>")
        destination = tmp_path / "views" / "valid"
        destination.mkdir()
        service._safe_extract(valid_zip, destination)
        assert (destination / "index.html").is_file()

        invalid_zip = tmp_path / "invalid.zip"
        with zipfile.ZipFile(invalid_zip, "w") as archive:
            archive.writestr("../escape.txt", "blocked")
        with pytest.raises(ValueError, match="非法路径"):
            service._safe_extract(invalid_zip, tmp_path / "views")
        assert not (tmp_path / "escape.txt").exists()
    finally:
        await service.stop()


def test_doc_resource_path_keeps_storage_prefix(tmp_path) -> None:
    repository = BigscreenRepository(database_config(tmp_path))
    service = BigscreenService(repository, str(tmp_path / "views"), "http://doc")
    resource = "upload/releases/screen package.zip"
    assert service._resource_url(resource) == (
        "http://doc/download/upload/releases/screen%20package.zip"
    )


@pytest.mark.asyncio
async def test_bigscreen_rows_are_isolated_by_tenant_and_project(tmp_path) -> None:
    repository = BigscreenRepository(database_config(tmp_path))
    service = BigscreenService(repository, str(tmp_path / "views"), "http://doc.invalid")
    await service.start()
    try:
        first = await service.save(
            {"viewName": "甲园区", "resourcePath": "a.zip", "projectId": "p1"},
            deploy=False,
            tenant_id="tenant-a",
            user_id="user-a",
        )
        await service.save(
            {"viewName": "乙园区", "resourcePath": "b.zip", "projectId": "p2"},
            deploy=False,
            tenant_id="tenant-b",
            user_id="user-b",
        )
        scoped = await repository.page({}, tenant_id="tenant-a", project_id="p1")
        assert scoped["total"] == 1
        assert scoped["contents"][0]["viewName"] == "甲园区"
        assert await repository.get(first["id"], "tenant-b") is None
        with pytest.raises(ValueError, match="无权访问"):
            await service.save(
                {"id": first["id"], "viewName": "越权"},
                deploy=False,
                tenant_id="tenant-b",
                user_id="user-b",
            )
    finally:
        await service.stop()


@pytest.mark.asyncio
async def test_direct_package_upload_deploys_root_index_and_rejects_oversize(tmp_path) -> None:
    repository = BigscreenRepository(database_config(tmp_path))
    service = BigscreenService(
        repository,
        str(tmp_path / "views"),
        "http://doc.invalid",
        max_package_bytes=256,
    )
    await service.start()
    try:
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, "w") as archive:
            archive.writestr("index.html", "<html>campus</html>")
        saved = await service.upload_package(
            Upload(buffer.getvalue()),
            {"viewName": "园区能源大屏", "projectId": "p1"},
            "tenant-a",
            "user-a",
        )
        assert saved["tenantId"] == "tenant-a"
        assert saved["projectId"] == "p1"
        assert (tmp_path / "views" / saved["viewUrl"] / "index.html").is_file()

        with pytest.raises(ValueError, match="体积超过限制"):
            await service.upload_package(
                Upload(b"x" * 257),
                {"viewName": "过大"},
                "tenant-a",
                "user-a",
            )
    finally:
        await service.stop()


@pytest.mark.asyncio
async def test_clean_preserves_package_and_reload_atomically_redeploys(tmp_path) -> None:
    repository = BigscreenRepository(database_config(tmp_path))
    service = BigscreenService(repository, str(tmp_path / "views"), "http://doc.invalid")
    await service.start()
    try:
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, "w") as archive:
            archive.writestr("index.html", "<html>original</html>")
        saved = await service.upload_package(
            Upload(buffer.getvalue()),
            {"viewName": "运行大屏", "projectId": "p1"},
            "tenant-a",
            "user-a",
        )
        archive_path = tmp_path / "views" / f"{saved['id']}.zip"
        view_index = tmp_path / "views" / saved["viewUrl"] / "index.html"
        assert service.deployment_status(saved) == {
            "deployed": True,
            "packageAvailable": True,
        }

        await service.clean(saved, "tenant-a")
        assert archive_path.is_file()
        assert not view_index.exists()
        assert service.deployment_status(saved)["deployed"] is False

        await service.reload(saved, "tenant-a")
        assert view_index.read_text() == "<html>original</html>"

        await service.delete(saved["id"], "tenant-a")
        assert not archive_path.exists()
        assert not view_index.exists()
    finally:
        await service.stop()


@pytest.mark.asyncio
async def test_bounded_private_history_rollback_retention_and_restart(tmp_path) -> None:
    repository = BigscreenRepository(database_config(tmp_path))
    service = BigscreenService(repository, str(tmp_path / "views"), "http://doc.invalid")
    await service.start()
    try:
        view = await service.upload_package(Upload(package_bytes("first")), {"viewName": "校园"}, "a", "u")
        # Concurrent requests serialize versions and never exceed the default cap.
        updates = await asyncio.gather(*(
            service.upload_package(Upload(package_bytes(str(i))), view, "a", "u") for i in range(4)
        ))
        assert [item["version"] for item in updates] == ["1.0.1", "1.0.2", "1.0.3", "1.0.4"]
        status = await service.storage_status(view, "a")
        assert status["keepVersions"] == 3
        assert [item["version"] for item in status["history"]] == ["1.0.3", "1.0.2"]
        assert not list(service.history.folder(view).glob("**/index.html"))
        assert service.views_dir not in service.history.root.parents
        assert len(list(service.history.folder(view).glob("*.zip"))) == 2
        await service.reload(view, "a")
        assert (await service.storage_status(view, "a"))["history"] == status["history"]
        revision = status["history"][1]["id"]
        reverted = await service.rollback({"id": view["id"], "revisionId": revision}, "a")
        assert reverted["version"] == "1.0.5"
        assert (service._view_dir(view) / "index.html").read_text() == "1"
        status = await service.storage_status(view, "a")
        assert [item["version"] for item in status["history"]] == ["1.0.4", "1.0.3"]
        status = await service.set_retention({"id": view["id"], "keepVersions": 1}, "a")
        assert status["history"] == [] and status["releasedBytes"] > 0
        assert (service._view_dir(view) / "index.html").read_text() == "1"
        with pytest.raises(ValueError, match="不存在或已淘汰"):
            await service.rollback({"id": view["id"], "revisionId": revision}, "a")
    finally:
        await service.stop()
    restarted = BigscreenService(BigscreenRepository(database_config(tmp_path)), str(tmp_path / "views"), "http://doc.invalid")
    await restarted.start()
    try:
        assert (await restarted.storage_status(view, "a"))["keepVersions"] == 1
    finally:
        await restarted.stop()


@pytest.mark.asyncio
async def test_failed_publication_and_corrupt_current_preserve_recovery(tmp_path, monkeypatch) -> None:
    repository = BigscreenRepository(database_config(tmp_path))
    service = BigscreenService(repository, str(tmp_path / "views"), "http://doc.invalid")
    await service.start()
    try:
        view = await service.upload_package(Upload(package_bytes("first")), {"viewName": "校园"}, "a", "u")
        view = await service.upload_package(Upload(package_bytes("current")), view, "a", "u")
        digest = checksum(service._archive_path(view))
        status = await service.storage_status(view, "a")
        with pytest.raises(ValueError, match="index.html"):
            await service.upload_package(Upload(package_bytes("bad", "missing.html")), view, "a", "u")
        async def fail_save(*args, **kwargs):
            raise RuntimeError("database unavailable")
        with monkeypatch.context() as patch:
            patch.setattr(repository, "save", fail_save)
            with pytest.raises(RuntimeError, match="database unavailable"):
                await service.upload_package(Upload(package_bytes("new")), view, "a", "u")
        assert (await service.storage_status(view, "a")) == status
        assert checksum(service._archive_path(view)) == digest
        assert (service._view_dir(view) / "index.html").read_text() == "current"
        assert not list(service.history.work.iterdir())
        # Metadata-first save must not leave an undeployable record either.
        with pytest.raises(ValueError, match="不存在"):
            await service.save({"viewName": "bad", "resourcePath": "local:missing.zip"}, tenant_id="a")
        assert (await repository.page({}, True))["total"] == 1
        service._archive_path(view).write_bytes(b"corrupt")
        for operation in (service.set_retention, service.prune_resources):
            with pytest.raises(ValueError, match="校验失败"):
                await operation({"id": view["id"], "keepVersions": 1, "includeLegacy": True}, "a")
        assert (await service.storage_status(view, "a"))["history"] == status["history"]
        assert (await service.storage_status(view, "a"))["keepVersions"] == 3
    finally:
        await service.stop()


@pytest.mark.asyncio
async def test_legacy_migration_protects_live_shared_paths_and_unrelated_files(tmp_path) -> None:
    service = BigscreenService(BigscreenRepository(database_config(tmp_path)), str(tmp_path / "views"), "http://doc.invalid")
    await service.start()
    try:
        view = await service.upload_package(Upload(package_bytes("current")), {"viewName": "校园"}, "a", "u")
        for i in range(4):
            backup = service.views_dir / f"{view['id']}.zip.bak-1.2.{i}-business-labels"
            backup.write_bytes(package_bytes(f"old-{i}" * 500))
            os.utime(backup, (time.time() - 100 + i, time.time() - 100 + i))
        old_dir = service.views_dir / f"{view['viewUrl']}.bak-20260907013710"
        old_dir.mkdir()
        (old_dir / "index.html").write_text("old")
        protected_dir = service.views_dir / f"{view['viewUrl']}.bak-20260907013738"
        protected_dir.mkdir()
        (protected_dir / "index.html").write_text("another live screen")
        await service.repository.save({"id": "other", "tenantId": "b", "viewUrl": protected_dir.name})
        unrelated = service.views_dir / f"{view['id']}.zip.bak-do-not-delete"
        unrelated.write_text("unrelated")
        before = await service.storage_status(view, "a")
        digest = checksum(service._archive_path(view))
        result = await service.prune_resources({"id": view["id"], "includeLegacy": True}, "a")
        assert result["releasedBytes"] == before["totalBytes"] - result["totalBytes"]
        assert [row["version"] for row in result["history"]] == ["1.2.3", "1.2.2"]
        assert result["legacyBackups"] == []
        assert not old_dir.exists()
        assert protected_dir.exists() and unrelated.exists()
        assert checksum(service._archive_path(view)) == digest
        assert (await service.prune_resources({"id": view["id"], "includeLegacy": True}, "a"))["releasedBytes"] == 0
        # Only known, stale uploads are disposable; rescue directories are not.
        stale = service.history.work / ("a" * 32 + ".upload")
        fresh = service.history.work / ("b" * 32 + ".upload")
        rescue = service.history.work / ("c" * 32)
        stale.write_bytes(b"old")
        fresh.write_bytes(b"new")
        rescue.mkdir()
        os.utime(stale, (time.time() - 90000, time.time() - 90000))
        assert service._cleanup_temporary() == 3
        assert not stale.exists() and fresh.exists() and rescue.exists()
        for count in (0, 6, True, "3", 2.5):
            with pytest.raises(ValueError, match="整数"):
                await service.set_retention({"id": view["id"], "keepVersions": count}, "a")
        for key in (".", "..", "../escape", "/"):
            with pytest.raises(ValueError):
                service._view_key(key)
        await service.repository.save({"id": "shared", "tenantId": "b", "viewUrl": view["viewUrl"]})
        with pytest.raises(ValueError, match="其他大屏引用"):
            await service.prune_resources({"id": view["id"], "includeLegacy": True}, "a")
        await service.delete("shared", "b")
        assert checksum(service._archive_path(view)) == digest
    finally:
        await service.stop()


@pytest.mark.asyncio
async def test_retention_endpoints_require_manager_and_scope_every_operation(tmp_path) -> None:
    import httpx
    from fastapi import FastAPI
    from jbm_cluster_py.platform.bigscreen.router import build_bigscreen_router
    service = BigscreenService(BigscreenRepository(database_config(tmp_path)), str(tmp_path / "views"), "http://doc.invalid")
    await service.start()
    try:
        view = await service.upload_package(Upload(package_bytes("private")), {"viewName": "校园"}, "a", "u")
        for operation in (service.storage_status, service.set_retention, service.prune_resources, service.rollback):
            with pytest.raises(ValueError, match="无权访问"):
                await operation({"id": view["id"], "keepVersions": 1}, "b")
        app = FastAPI()
        @app.middleware("http")
        async def identity(request, call_next):
            request.state.identity = {"tenantId": "a", "userId": "u", "roles": []}
            return await call_next(request)
        app.include_router(build_bigscreen_router(service.repository, service))
        async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
            for endpoint in ("storage", "retention", "prune", "rollback"):
                response = await client.post(f"/bigscreenView/{endpoint}", json={"id": view["id"]})
                assert response.status_code == 403
    finally:
        await service.stop()
