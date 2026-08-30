from __future__ import annotations

import io
import zipfile

import pytest
from jbm_cluster_py.platform.bigscreen.repository import BigscreenRepository
from jbm_cluster_py.platform.bigscreen.service import BigscreenService
from sqlalchemy.pool import NullPool


class Upload:
    def __init__(self, data: bytes, filename: str = "screen.zip") -> None:
        self.filename = filename
        self._stream = io.BytesIO(data)

    async def read(self, size: int = -1) -> bytes:
        return self._stream.read(size)


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
