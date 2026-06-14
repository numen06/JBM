from __future__ import annotations

from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

from pydantic import Field

from jbm_cluster_py.common.masterdata import LooseModel, PageForm


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


class DocPathForm(LooseModel):
    paths: List[str] = Field(default_factory=list)
    ids: List[str] = Field(default_factory=list)


class FileReqBody(LooseModel):
    name: Optional[str] = None
    id: Optional[str] = None
    offset: int = 0
    count: int = 10


class BaseDocModel(LooseModel):
    doc_id: Optional[str] = Field(default=None, alias="docId")
    doc_name: Optional[str] = Field(default=None, alias="docName")
    size: Optional[int] = None
    doc_group_id: Optional[str] = Field(default=None, alias="docGroupId")
    doc_group: Optional[str] = Field(default=None, alias="docGroup")
    doc_path: Optional[str] = Field(default=None, alias="docPath")
    state: Optional[str] = None
    content_type: Optional[str] = Field(default=None, alias="contentType")
    effective_time: Optional[int] = Field(default=None, alias="effectiveTime")
    expiration_time: Optional[str] = Field(default=None, alias="expirationTime")
    version: Optional[Any] = None
    creator: Optional[Any] = None
    create_time: Optional[str] = Field(default=None, alias="createTime")
    update_time: Optional[str] = Field(default=None, alias="updateTime")


class BaseDocGroupModel(LooseModel):
    group_id: Optional[str] = Field(default=None, alias="groupId")
    group_path: Optional[str] = Field(default=None, alias="groupPath")
    expiration_time: Optional[str] = Field(default=None, alias="expirationTime")
    auto_clear: Optional[bool] = Field(default=None, alias="autoClear")
    max_quantity: Optional[int] = Field(default=None, alias="maxQuantity")
    token_key: Optional[str] = Field(default=None, alias="tokenKey")
    doc_group_name: Optional[str] = Field(default=None, alias="docGroupName")
    create_time: Optional[str] = Field(default=None, alias="createTime")
    update_time: Optional[str] = Field(default=None, alias="updateTime")


class BaseDocTokenModel(LooseModel):
    token_key: Optional[str] = Field(default=None, alias="tokenKey")
    expiration_time: Optional[str] = Field(default=None, alias="expirationTime")
    effective_time: Optional[int] = Field(default=None, alias="effectiveTime")
    effective_time_type: Optional[int] = Field(default=None, alias="effectiveTimeType")
    doc_group_id: Optional[str] = Field(default=None, alias="docGroupId")
    doc_id: Optional[str] = Field(default=None, alias="docId")
    create_time: Optional[str] = Field(default=None, alias="createTime")
    update_time: Optional[str] = Field(default=None, alias="updateTime")


class MasterDataBody(LooseModel):
    baseDoc: Optional[Dict[str, Any]] = None
    baseDocGroup: Optional[Dict[str, Any]] = None
    baseDocToken: Optional[Dict[str, Any]] = None
    page_form: PageForm = Field(default_factory=PageForm, alias="pageForm")
