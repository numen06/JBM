from __future__ import annotations

from typing import Any

from fastapi import APIRouter, Body
from fastapi.responses import RedirectResponse
from jbm_cluster_py.common.result import ok
from jbm_cluster_py.platform.bigscreen.repository import BigscreenRepository
from jbm_cluster_py.platform.bigscreen.service import BigscreenService


def build_bigscreen_router(repository: BigscreenRepository, service: BigscreenService) -> APIRouter:
    router = APIRouter()

    @router.get("/view/{view}", tags=["大屏预览"])
    async def view(view: str) -> RedirectResponse:
        return RedirectResponse(f"/static/{service._view_key(view)}/index.html")

    @router.post("/bigscreenView/pageList", tags=["大屏管理"])
    async def page_list(body: dict[str, Any] | None = Body(default=None)) -> dict[str, Any]:
        return ok(await repository.page(body or {}), "查询分页列表成功")

    @router.post("/bigscreenView/list", tags=["大屏管理"])
    async def list_rows(body: dict[str, Any] | None = Body(default=None)) -> dict[str, Any]:
        return ok((await repository.page(body or {}, True))["contents"], "查询列表成功")

    @router.post("/bigscreenView/model", tags=["大屏管理"])
    async def model(body: dict[str, Any]) -> dict[str, Any]:
        return ok(await repository.get(str(body.get("id") or "")), "查询对象成功")

    @router.post("/bigscreenView/save", tags=["大屏管理"])
    async def save(body: dict[str, Any]) -> dict[str, Any]:
        return ok(await service.save(body), "保存对象成功")

    @router.post("/bigscreenView/saveBatch", tags=["大屏管理"])
    async def save_batch(body: Any = Body(...)) -> dict[str, Any]:
        values = body if isinstance(body, list) else body.get("list") or body.get("contents") or []
        return ok([await service.save(item) for item in values], "保存对象成功")

    @router.post("/bigscreenView/delete", tags=["大屏管理"])
    async def delete(body: dict[str, Any]) -> dict[str, Any]:
        return ok(await service.delete(str(body.get("id") or "")), "删除对象成功")

    @router.post("/bigscreenView/deleteByIds", tags=["大屏管理"])
    async def delete_ids(body: dict[str, Any]) -> dict[str, Any]:
        for view_id in body.get("ids") or []:
            await service.delete(str(view_id))
        return ok(True, "批量成功删除")

    @router.post("/bigscreenView/upload", tags=["大屏管理"])
    async def upload(body: dict[str, Any]) -> dict[str, Any]:
        return ok(await service.upload(body), "上载大屏包成功")

    @router.post("/bigscreenView/isUpload", tags=["大屏管理"])
    async def is_upload(body: dict[str, Any]) -> dict[str, Any]:
        return ok(await service.is_uploaded(body), "判断成功")

    @router.post("/bigscreenView/cleanView", tags=["大屏管理"])
    async def clean(body: dict[str, Any]) -> dict[str, Any]:
        return ok(await service.clean(body), "清理视图成功")

    return router
