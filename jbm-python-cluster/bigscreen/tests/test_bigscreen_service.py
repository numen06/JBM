from __future__ import annotations

import zipfile

import pytest
from jbm_cluster_py.platform.bigscreen.repository import BigscreenRepository
from jbm_cluster_py.platform.bigscreen.service import BigscreenService


def database_config(tmp_path) -> dict[str, str]:
    return {"url": f"sqlite+aiosqlite:///{(tmp_path / 'bigscreen.db').as_posix()}"}


@pytest.mark.asyncio
async def test_bigscreen_metadata_inheritance_and_delete_guard(tmp_path) -> None:
    repository = BigscreenRepository(database_config(tmp_path))
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
