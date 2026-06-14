from __future__ import annotations

from datetime import datetime, timezone
from typing import Any, Dict, Iterable, Mapping, Optional

from pydantic import BaseModel, ConfigDict, Field


class LooseModel(BaseModel):
    model_config = ConfigDict(extra="allow", populate_by_name=True)


class PageForm(LooseModel):
    curr_page: int = Field(default=1, alias="currPage")
    page_size: int = Field(default=10, alias="pageSize")


class PageRequestBody(LooseModel):
    page_form: PageForm = Field(default_factory=PageForm, alias="pageForm")


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def model_dump_compat(value: Any) -> Dict[str, Any]:
    if value is None:
        return {}
    if isinstance(value, BaseModel):
        return value.model_dump(by_alias=True, exclude_none=True)
    if isinstance(value, Mapping):
        return dict(value)
    return {}


def first_payload(body: Optional[Mapping[str, Any]], aliases: Iterable[str]) -> Dict[str, Any]:
    if not body:
        return {}
    for alias in aliases:
        value = body.get(alias)
        if isinstance(value, Mapping):
            return dict(value)
    return dict(body)


def page_form_from_body(body: Optional[Mapping[str, Any]]) -> PageForm:
    if not body:
        return PageForm()
    page_form = body.get("pageForm") or body.get("page_form") or {}
    if isinstance(page_form, Mapping):
        return PageForm(**page_form)
    return PageForm()


def java_page(contents: list[dict[str, Any]], total: int, page_form: PageForm) -> Dict[str, Any]:
    return {
        "contents": contents,
        "total": total,
        "currPage": max(int(page_form.curr_page or 1), 1),
        "pageSize": max(int(page_form.page_size or 10), 1),
    }
