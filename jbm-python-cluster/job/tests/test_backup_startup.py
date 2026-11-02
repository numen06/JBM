import asyncio
from types import SimpleNamespace
from unittest.mock import Mock

import httpx
import pytest
from fastapi import FastAPI

from jbm_cluster_py.platform.job.database_backup_router import build_database_backup_router
from jbm_cluster_py.platform.job import main


@pytest.mark.asyncio
async def test_busy_previous_owner_is_retried_then_automatically_acquired(monkeypatch):
    app = SimpleNamespace(state=SimpleNamespace(database_backups=None, database_backup_status="starting"))
    entered = asyncio.Event()
    release = asyncio.Event()
    async def work():
        entered.set()
        await release.wait()
    manager = Mock(worker=work)
    factory = Mock(side_effect=[BlockingIOError("previous owner"), manager])
    monkeypatch.setattr(main, "DatabaseBackups", factory)
    # Avoid waiting two wall-clock seconds while preserving an async cancellation point.
    sleep = asyncio.sleep
    async def retry(_):
        assert app.state.database_backup_status == "waiting-for-owner"
        assert app.state.database_backups is None
        await sleep(0)
    monkeypatch.setattr(main.asyncio, "sleep", retry)
    task = asyncio.create_task(main.run_backup_runtime(app, "/tmp/test", {}))
    await asyncio.wait_for(entered.wait(), timeout=2)
    assert factory.call_count == 2
    assert app.state.database_backups is manager
    assert app.state.database_backup_status == "ready"
    manager.worker_lock.close.assert_not_called()
    task.cancel()
    with pytest.raises(asyncio.CancelledError):
        await task
    manager.worker_lock.close.assert_called_once()


@pytest.mark.asyncio
async def test_permanent_startup_failure_is_visible_to_restart_policy(monkeypatch):
    app = SimpleNamespace(state=SimpleNamespace(database_backups=None, database_backup_status="starting"))
    monkeypatch.setattr(main, "DatabaseBackups", Mock(side_effect=ValueError("bad configuration")))
    await main.run_backup_runtime(app, "/tmp/test", {})
    assert app.state.database_backup_status == "failed"
    assert app.state.database_backups is None


@pytest.mark.asyncio
async def test_standby_backup_api_is_temporarily_unavailable_without_enqueue():
    app = FastAPI()
    app.state.database_backups = None
    app.include_router(build_database_backup_router())
    @app.middleware("http")
    async def platform_identity(request, call_next):
        request.state.identity = {"userId": "test", "roles": ["platform_operator"]}
        return await call_next(request)
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        response = await client.get("/sysJob/database-backups/sources")
    assert response.status_code == 503
    assert response.headers["Retry-After"] == "2"


@pytest.mark.asyncio
async def test_shutdown_keeps_ownership_until_inflight_step_finishes(monkeypatch):
    import threading
    from jbm_cluster_py.platform.job.database_backups import DatabaseBackups
    entered = threading.Event()
    release = threading.Event()
    manager = object.__new__(DatabaseBackups)
    manager.schedule = lambda: None
    def operation():
        entered.set()
        release.wait(timeout=2)
    manager.run_next = operation
    task = asyncio.create_task(manager.worker())
    await asyncio.to_thread(entered.wait, 2)
    assert entered.is_set()
    task.cancel()
    await asyncio.sleep(0.01)
    assert not task.done()
    release.set()
    with pytest.raises(asyncio.CancelledError):
        await task
