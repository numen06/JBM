"""Presentation metadata lives with the existing JBM resource, not in a second catalogue."""

from __future__ import annotations

import json
import math
from collections.abc import Mapping


def config(view):
    value = view.get("configData")
    if isinstance(value, str):
        try:
            value = json.loads(value)
        except ValueError:
            value = {}
    return dict(value) if isinstance(value, Mapping) else {}


def is_campus(view):
    return config(view).get("kind") == "campus3d"


def visible(view):
    return not is_campus(view) or config(view).get("status") == "published"


def normalize_manifest(value):
    if (
        not isinstance(value, dict)
        or value.get("kind") != "campus3d"
        or value.get("protocol") != "campus-v1"
    ):
        raise ValueError("三维资源包需声明 campus3d / campus-v1 协议")
    ids = value.get("modelIds")
    if not isinstance(ids, list) or not ids or len(ids) > 2000:
        raise ValueError("三维资源包缺少楼体标识")
    if any(not isinstance(i, str) or not i or len(i) > 100 for i in ids) or len(set(ids)) != len(
        ids
    ):
        raise ValueError("三维楼体标识必须唯一且有效")
    return {"kind": "campus3d", "protocol": "campus-v1", "modelIds": ids}


def normalize_settings(view, body):
    current = config(view)
    if not is_campus(view):
        raise ValueError("当前资源不是三维园区")
    status = body.get("status", current.get("status", "draft"))
    if status not in {"draft", "published", "disabled"}:
        raise ValueError("场景状态无效")
    if not str(view.get("projectId") or "").strip():
        raise ValueError("请先绑定项目")
    bindings = body.get("bindings", current.get("bindings", {}))
    if not isinstance(bindings, dict) or any(
        k not in current.get("modelIds", []) or not isinstance(v, str) or not v or len(v) > 64
        for k, v in bindings.items()
    ):
        raise ValueError("建筑绑定无效")
    if len(set(bindings.values())) != len(bindings):
        raise ValueError("同一建筑不能绑定多个楼体")
    camera = body.get("camera", current.get("camera"))
    if camera is not None:
        if not isinstance(camera, dict) or any(
            not isinstance(camera.get(k), list)
            or len(camera[k]) != 3
            or any(
                isinstance(n, bool)
                or not isinstance(n, (int, float))
                or not math.isfinite(n)
                or abs(n) > 1e7
                for n in camera[k]
            )
            for k in ("position", "target")
        ):
            raise ValueError("默认视角无效")
        camera = {k: camera[k] for k in ("position", "target")}
    return {
        **current,
        "status": status,
        "isDefault": body.get("isDefault", current.get("isDefault", False)) is True,
        "bindings": bindings,
        "camera": camera,
    }
