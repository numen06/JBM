"""Private, deployment-local feedback shared by all JBM host applications."""
from datetime import UTC, datetime
from typing import Literal
from uuid import uuid4

import sqlalchemy as sa
from fastapi import APIRouter, HTTPException, Query, Request
from pydantic import BaseModel, ConfigDict, Field, field_validator

from jbm_cluster_py.common.api_access import is_platform
from jbm_cluster_py.common.result import ok

metadata = sa.MetaData()
feedback = sa.Table(
    "jbm_feedback", metadata,
    sa.Column("id", sa.String(32), primary_key=True),
    sa.Column("user_id", sa.BigInteger, nullable=False),
    sa.Column("tenant_id", sa.BigInteger, nullable=False),
    sa.Column("app_id", sa.BigInteger, nullable=False),
    sa.Column("username", sa.String(100), nullable=False),
    sa.Column("deployment_mode", sa.String(16), nullable=False),
    sa.Column("feedback_type", sa.String(16), nullable=False),
    sa.Column("title", sa.String(120), nullable=False),
    sa.Column("description", sa.Text, nullable=False),
    sa.Column("contact", sa.String(200), nullable=False),
    sa.Column("source_page", sa.String(500), nullable=False),
    sa.Column("status", sa.String(16), nullable=False),
    sa.Column("handle_note", sa.Text, nullable=False),
    sa.Column("handled_by", sa.BigInteger),
    sa.Column("revision", sa.Integer, nullable=False),
    sa.Column("created_at", sa.DateTime, nullable=False),
    sa.Column("updated_at", sa.DateTime, nullable=False),
    sa.Index("ix_jbm_feedback_created", "created_at", "id"),
)


class SubmitFeedback(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)
    title: str = Field(min_length=1, max_length=120)
    feedbackType: Literal["suggestion", "problem", "other"] = "suggestion"
    description: str = Field(min_length=1, max_length=5000)
    contact: str = Field(default="", max_length=200)
    sourcePage: str = Field(default="", max_length=500)

    @field_validator("sourcePage")
    @classmethod
    def safe_page(cls, value):
        # Keep page context, never URL query tokens or off-site URLs.
        if value and (not value.startswith("/") or value.startswith("//")):
            raise ValueError("来源页面必须为站内路径")
        return value.split("?", 1)[0].split("#", 1)[0]


class HandleFeedback(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)
    status: Literal["pending", "processing", "resolved"]
    handleNote: str = Field(default="", max_length=2000)
    revision: int = Field(ge=1)


def user_identity(request):
    identity = request.state.identity
    if not identity.get("userId") or not identity.get("tenantId"):
        raise HTTPException(403, "请使用已登录的用户账号提交意见反馈")
    return identity


def operator(request):
    identity = user_identity(request)
    if not is_platform(identity):
        raise HTTPException(403, "仅平台运营或超级管理员可以查看和处理反馈")
    return identity


def item(row):
    return {
        "id": row["id"], "userId": str(row["user_id"]), "tenantId": str(row["tenant_id"]),
        "appId": str(row["app_id"]), "username": row["username"],
        "deploymentMode": row["deployment_mode"], "feedbackType": row["feedback_type"],
        "title": row["title"], "description": row["description"], "contact": row["contact"],
        "sourcePage": row["source_page"], "status": row["status"], "handleNote": row["handle_note"],
        "handledBy": str(row["handled_by"]) if row["handled_by"] else None,
        "revision": row["revision"],
        "createdAt": row["created_at"].isoformat() + "Z",
        "updatedAt": row["updated_at"].isoformat() + "Z",
    }


def build_feedback_router(engine, settings):
    mode = str(settings.get("deployment-mode") or "local")
    if mode not in {"local", "cloud"}:
        raise ValueError("jbm.feedback.deployment-mode 必须为 local 或 cloud")
    router = APIRouter(prefix="/feedback", tags=["意见反馈"])

    @router.get("/context")
    async def context(request: Request):
        identity = user_identity(request)
        return ok({"deploymentMode": mode, "canManage": is_platform(identity)})

    @router.post("/submit")
    async def submit(body: SubmitFeedback, request: Request):
        identity = user_identity(request)
        now = datetime.now(UTC).replace(tzinfo=None)
        key = uuid4().hex
        async with engine.begin() as conn:
            await conn.execute(feedback.insert().values(
                id=key, user_id=int(identity["userId"]), tenant_id=int(identity["tenantId"]),
                app_id=int(identity.get("appId") or 0), username=str(identity.get("username") or identity.get("userName") or identity["userId"]),
                deployment_mode=mode, feedback_type=body.feedbackType, title=body.title,
                description=body.description, contact=body.contact, source_page=body.sourcePage,
                status="pending", handle_note="", revision=1, created_at=now, updated_at=now,
            ))
        # A receipt is deliberately the only feedback data exposed to submitters.
        return ok({"id": key})

    @router.get("")
    async def listing(request: Request, page: int = Query(1, ge=1), size: int = Query(20, ge=1, le=100),
                      status: Literal["pending", "processing", "resolved"] | None = None):
        operator(request)
        condition = feedback.c.status == status if status else sa.true()
        async with engine.connect() as conn:
            total = await conn.scalar(sa.select(sa.func.count()).select_from(feedback).where(condition))
            rows = (await conn.execute(sa.select(feedback).where(condition).order_by(
                feedback.c.created_at.desc(), feedback.c.id.desc()).offset((page-1)*size).limit(size))).mappings().all()
        return ok({"items": [item(row) for row in rows], "total": total, "page": page, "size": size})

    @router.get("/{feedback_id}")
    async def detail(feedback_id: str, request: Request):
        operator(request)
        async with engine.connect() as conn:
            row = (await conn.execute(sa.select(feedback).where(feedback.c.id == feedback_id))).mappings().first()
        if row is None:
            raise HTTPException(404, "反馈不存在")
        return ok(item(row))

    @router.put("/{feedback_id}")
    async def handle(feedback_id: str, body: HandleFeedback, request: Request):
        identity = operator(request)
        async with engine.begin() as conn:
            result = await conn.execute(feedback.update().where(feedback.c.id == feedback_id,
                feedback.c.revision == body.revision).values(status=body.status, handle_note=body.handleNote,
                handled_by=int(identity["userId"]), revision=body.revision+1,
                updated_at=datetime.now(UTC).replace(tzinfo=None)))
            if result.rowcount != 1:
                exists = await conn.scalar(sa.select(feedback.c.id).where(feedback.c.id == feedback_id))
                raise HTTPException(409 if exists else 404, "反馈已更新，请刷新后再处理" if exists else "反馈不存在")
        return ok()

    return router
