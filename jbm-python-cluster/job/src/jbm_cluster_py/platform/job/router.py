from __future__ import annotations

from typing import Any, Dict, Optional

from fastapi import APIRouter, Body

from jbm_cluster_py.common.result import fail, ok
from jbm_cluster_py.platform.job.service import JobService


def build_job_router(service: JobService) -> APIRouter:
    router = APIRouter()

    @router.get("/sysJob/test")
    async def test() -> Dict[str, Any]:
        return ok(True)

    @router.post("/sysJob/pageList")
    async def page_jobs(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok(service.page_jobs(body), "查询分页列表成功")

    @router.get("/sysJob/{job_id:int}")
    async def get_job(job_id: int) -> Dict[str, Any]:
        row = service.get_job(job_id)
        if row is None:
            return fail(None, "定时任务不存在", 404)
        return ok(row, "查询对象成功")

    @router.post("/sysJob")
    @router.post("/sysJob/add")
    async def create_job(body: Dict[str, Any] = Body(default_factory=dict)) -> Dict[str, Any]:
        return ok(service.create_job(body), "新增定时任务成功")

    @router.put("/sysJob/{job_id:int}")
    async def update_job(job_id: int, body: Dict[str, Any] = Body(default_factory=dict)) -> Dict[str, Any]:
        updated = service.update_job(job_id, body)
        if not updated:
            return fail(False, "定时任务不存在", 404)
        return ok(True, "修改定时任务成功")

    @router.post("/sysJob/edit")
    async def edit_job(body: Dict[str, Any] = Body(default_factory=dict)) -> Dict[str, Any]:
        job_id = int(body.get("jobId") or body.get("id") or 0)
        return await update_job(job_id, body)

    @router.delete("/sysJob/{job_id:int}")
    async def delete_job(job_id: int) -> Dict[str, Any]:
        return ok(service.delete_job(job_id), "删除定时任务成功")

    @router.put("/sysJob/changeStatus")
    async def change_status(body: Dict[str, Any] = Body(default_factory=dict)) -> Dict[str, Any]:
        row = service.change_status(body)
        if row is None:
            return fail(None, "定时任务不存在", 404)
        return ok(row, "状态修改成功")

    @router.put("/sysJob/run")
    async def run_job(body: Dict[str, Any] = Body(default_factory=dict)) -> Dict[str, Any]:
        row = service.run_job(body)
        if row is None:
            return fail(None, "定时任务不存在", 404)
        return ok(row, "执行成功")

    @router.post("/sysJobLog/pageList")
    async def page_job_logs(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok(service.page_logs(body), "查询分页列表成功")

    @router.delete("/sysJobLog/clean")
    async def clean_logs() -> Dict[str, Any]:
        service.clean_logs()
        return ok(True, "清空日志成功")

    return router
