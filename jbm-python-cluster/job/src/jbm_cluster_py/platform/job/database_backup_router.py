from __future__ import annotations

import asyncio
from functools import partial

from fastapi import APIRouter, Body, Depends, HTTPException, Query, Request
from fastapi.responses import FileResponse

from jbm_cluster_py.common.api_access import is_platform
from jbm_cluster_py.common.result import ok


def operator(request: Request):
    identity = getattr(request.state, "identity", {})
    if not is_platform(identity):
        raise HTTPException(403, "仅平台运维管理员可操作数据库")
    return str(identity.get("userId") or identity.get("user_id") or identity.get("sub") or "platform")


def build_database_backup_router():
    # Reuse the existing platform-only /sysJob resource boundary at both gateway and service.
    router = APIRouter(prefix="/sysJob/database-backups", dependencies=[Depends(operator)])

    async def call(function, *args, **kwargs):
        try:
            return await asyncio.to_thread(partial(function, *args, **kwargs))
        except (ValueError, TypeError, KeyError) as exc:
            raise HTTPException(422, str(exc)) from None

    def service(request):
        return request.app.state.database_backups

    @router.get("/sources")
    async def sources(request: Request):
        return ok(await call(service(request).sources))

    @router.post("/sources")
    async def save_source(request: Request, body: dict = Body(...), actor=Depends(operator)):
        source_id = await call(service(request).save_source, body)
        await call(service(request).audit, actor, "save_source", source_id)
        return ok(await call(service(request).test_source, source_id))

    @router.post("/sources/{source_id}/test")
    async def test_source(source_id: str, request: Request, actor=Depends(operator)):
        result = await call(service(request).test_source, source_id)
        await call(service(request).audit, actor, "test_connection", source_id)
        return ok(result)

    @router.get("/sources/{source_id}/databases")
    async def databases(source_id: str, request: Request):
        return ok(await call(service(request).discover_databases, source_id))

    @router.post("/sources/{source_id}/databases")
    async def attach_database(source_id: str, request: Request, body: dict = Body(...), actor=Depends(operator)):
        result = await call(service(request).attach_database, source_id, body.get("database"))
        await call(service(request).audit, actor, "attach_database", result["id"])
        return ok(result)

    @router.put("/sources/{source_id}/policy")
    async def save_policy(source_id: str, request: Request, body: dict = Body(...), actor=Depends(operator)):
        policy = await call(service(request).save_policy, source_id, body)
        await call(service(request).audit, actor, "save_policy", source_id)
        return ok(policy)

    @router.post("/backups")
    async def backup(request: Request, body: dict = Body(...), actor=Depends(operator)):
        return ok(await call(service(request).enqueue, "backup", body.get("sourceId"), actor, {"origin": "manual"}))

    @router.get("/tasks")
    async def tasks(request: Request, kind: str = Query("backup", pattern="^(backup|restore)$"),
                    page: int = Query(1, ge=1), size: int = Query(20, ge=1, le=100),
                    sourceId: str | None = Query(None)):
        return ok(await call(service(request).tasks, kind, page, size, sourceId))

    @router.get("/tasks/{task_id}")
    async def task(task_id: str, request: Request):
        return ok(await call(service(request).task, task_id))

    @router.get("/backups/{task_id}/file")
    async def download(task_id: str, request: Request, actor=Depends(operator)):
        backup = await call(service(request).task, task_id)
        path = await call(service(request).backup_file, backup)
        await call(service(request).audit, actor, "download", task_id)
        return FileResponse(path, media_type="application/gzip", filename=backup["filename"],
                            headers={"Cache-Control": "no-store", "X-Content-Type-Options": "nosniff"})

    @router.delete("/backups/{task_id}")
    async def delete(task_id: str, request: Request, actor=Depends(operator)):
        await call(service(request).delete_backup, task_id, actor)
        return ok(True)

    @router.post("/restores")
    async def restore(request: Request, body: dict = Body(...), actor=Depends(operator)):
        manager = service(request)
        options = await call(manager.restore_options, body.get("backupId"), body.get("mode"),
                             body.get("targetDatabase"), body.get("confirmation"), body.get("maintenanceConfirmed"))
        backup = await call(manager.task, body["backupId"])
        return ok(await call(manager.enqueue, "restore", backup["sourceId"], actor, options))

    return router
