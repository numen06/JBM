import io
import json
import zipfile

import pytest
from fastapi import FastAPI, Request
from httpx import ASGITransport, AsyncClient
from jbm_cluster_py.platform.bigscreen.repository import BigscreenRepository
from jbm_cluster_py.platform.bigscreen.router import build_bigscreen_router
from jbm_cluster_py.platform.bigscreen.service import BigscreenService


class Upload:
    filename = "campus.zip"

    def __init__(self, ids=("B01", "B02"), title="scene"):
        self.buffer = io.BytesIO()
        with zipfile.ZipFile(self.buffer, "w") as package:
            package.writestr("index.html", title)
            package.writestr(
                "scene.json",
                json.dumps({"kind": "campus3d", "protocol": "campus-v1", "modelIds": ids}),
            )
        self.buffer.seek(0)

    async def read(self, size):
        return self.buffer.read(size)


@pytest.mark.asyncio
async def test_campus_lifecycle_default_tenant_isolation_and_reader_visibility(tmp_path):
    repo = BigscreenRepository({"url": f"sqlite+aiosqlite:///{tmp_path.as_posix()}/test.db"})
    service = BigscreenService(repo, str(tmp_path / "views"), "http://unused")
    await service.start()
    try:
        first = await service.upload_package(
            Upload(), {"viewName": "园区一期", "projectId": "10"}, "1", "admin"
        )
        second = await service.upload_package(
            Upload(), {"viewName": "园区二期", "projectId": "10"}, "1", "admin"
        )
        other = await service.upload_package(
            Upload(), {"viewName": "其他租户", "projectId": "10"}, "2", "admin"
        )
        assert json.loads(first["configData"])["status"] == "draft"
        with pytest.raises(ValueError, match="无权"):
            await service.configure_presentation({"id": first["id"], "status": "published"}, "2")
        with pytest.raises(ValueError, match="同一建筑"):
            await service.configure_presentation(
                {"id": first["id"], "bindings": {"B01": "9", "B02": "9"}}, "1"
            )
        with pytest.raises(ValueError, match="绑定"):
            await service.configure_presentation(
                {"id": first["id"], "bindings": {"FOREIGN": "9"}}, "1"
            )
        for view, tenant in ((first, "1"), (other, "2"), (second, "1")):
            await service.configure_presentation(
                {"id": view["id"], "status": "published", "isDefault": True}, tenant
            )
        assert not json.loads((await repo.get(first["id"], "1"))["configData"])["isDefault"]
        assert json.loads((await repo.get(other["id"], "2"))["configData"])["isDefault"]
        # New package is a draft until explicitly published; bindings survive valid IDs.
        await service.configure_presentation(
            {"id": first["id"], "bindings": {"B01": "9007199254740993"}}, "1"
        )
        first = await service.upload_package(
            Upload(title="updated"),
            {"id": first["id"], "viewName": "一期", "projectId": "10"},
            "1",
            "admin",
        )
        assert json.loads(first["configData"])["bindings"] == {"B01": "9007199254740993"}
        assert json.loads(first["configData"])["status"] == "draft"
        app = FastAPI()

        @app.middleware("http")
        async def identity(request: Request, call_next):
            request.state.identity = {"tenantId": "1", "userId": "reader", "roles": []}
            return await call_next(request)

        app.include_router(build_bigscreen_router(repo, service))
        async with AsyncClient(transport=ASGITransport(app), base_url="http://test") as client:
            result = await client.post("/bigscreenView/list", json={})
            assert [r["id"] for r in result.json()["result"]] == [second["id"]]
            assert (
                await client.post("/bigscreenView/model", json={"id": first["id"]})
            ).status_code == 404
            assert (
                await client.post(
                    "/bigscreenView/presentation", json={"id": second["id"], "status": "disabled"}
                )
            ).status_code == 403
            await service.clean({"id": second["id"]}, "1")
            rows = (await client.post("/bigscreenView/list", json={})).json()["result"]
            assert rows[0]["deployed"] is False
    finally:
        await service.stop()
