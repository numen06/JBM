"""Bounded ZIP history outside the public static tree; no duplicated extracted assets."""
from __future__ import annotations

import hashlib
import json
import re
import shutil
import uuid
from collections.abc import Mapping
from datetime import UTC, datetime
from pathlib import Path
from typing import Any


def storage_key(value: Any) -> str:
    key = str(value or "").strip()
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{0,127}", key):
        raise ValueError("不是合法的大屏资源标识")
    return key


def checksum(path: Path) -> str:
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def byte_size(path: Path) -> int:
    if path.is_symlink() or not path.exists():
        return 0
    if path.is_file():
        return path.stat().st_size
    return sum(item.stat().st_size for item in path.rglob("*")
               if item.is_file() and not item.is_symlink())


def extension(view: Mapping[str, Any]) -> dict[str, Any]:
    value = view.get("extendData")
    if isinstance(value, str):
        try:
            value = json.loads(value)
        except ValueError:
            value = {}
    return dict(value) if isinstance(value, Mapping) else {}


def keep_versions(view: Mapping[str, Any]) -> int:
    policy = extension(view).get("resourceRetention")
    count = policy.get("keepVersions") if isinstance(policy, Mapping) else None
    return count if type(count) is int and 1 <= count <= 5 else 3


class PackageHistory:
    def __init__(self, views_dir: Path) -> None:
        self.root = views_dir.parent / f".{views_dir.name}-history"
        self.work = self.root / ".work"

    def start(self) -> None:
        if self.root.is_symlink() or self.work.is_symlink():
            raise ValueError("历史资源目录不允许符号链接")
        self.work.mkdir(parents=True, exist_ok=True)

    def folder(self, view: Mapping[str, Any]) -> Path:
        path = self.root / storage_key(view["id"])
        if self.root.is_symlink() or path.is_symlink() or path.resolve().parent != self.root.resolve():
            raise ValueError("非法历史资源路径")
        return path

    def archive(self, view: Mapping[str, Any], revision: str) -> Path:
        if not re.fullmatch(r"[a-f0-9]{32}", revision):
            raise ValueError("非法历史版本标识")
        folder = self.folder(view)
        result = folder / f"{revision}.zip"
        if result.is_symlink() or result.resolve().parent != folder.resolve():
            raise ValueError("非法历史资源路径")
        return result

    def entries(self, view: Mapping[str, Any]) -> list[dict[str, Any]]:
        result = []
        folder = self.folder(view)
        if not folder.exists():
            return result
        for metadata in folder.glob("*.json"):
            if metadata.is_symlink() or not re.fullmatch(r"[a-f0-9]{32}", metadata.stem):
                continue
            try:
                row = json.loads(metadata.read_text(encoding="utf-8"))
                if not isinstance(row, dict) or not re.fullmatch(r"[a-f0-9]{64}", str(row.get("sha256") or "")):
                    continue
                archive = self.archive(view, metadata.stem)
                if (not archive.is_file() or row.get("id") != metadata.stem
                        or row.get("viewId") != str(view["id"])
                        or str(row.get("tenantId") or "") != str(view.get("tenantId") or "")):
                    continue
                row["sizeBytes"] = archive.stat().st_size
                result.append(row)
            except (ValueError, OSError, TypeError):
                continue
        return sorted(result, key=lambda item: (str(item.get("createdAt") or ""), item["id"]), reverse=True)

    def remember(self, view: Mapping[str, Any], archive: Path, *,
                 created_at: str | None = None, version: str | None = None) -> str | None:
        digest = checksum(archive)
        if any(row["sha256"] == digest for row in self.entries(view)):
            return None
        folder = self.folder(view)
        folder.mkdir(parents=True, exist_ok=True)
        revision = uuid.uuid4().hex
        target = self.archive(view, revision)
        metadata = folder / f"{revision}.json"
        row = {"id": revision, "viewId": str(view["id"]), "tenantId": view.get("tenantId"),
               "version": version or view.get("version"), "sha256": digest,
               "createdAt": created_at or datetime.now(UTC).isoformat()}
        try:
            shutil.copyfile(archive, target)
            metadata.write_text(json.dumps(row, ensure_ascii=False), encoding="utf-8")
        except BaseException:
            target.unlink(missing_ok=True)
            metadata.unlink(missing_ok=True)
            raise
        return revision

    def forget(self, view: Mapping[str, Any], revision: str) -> int:
        archive = self.archive(view, revision)
        metadata = archive.with_suffix(".json")
        if metadata.is_symlink():
            raise ValueError("非法历史元数据路径")
        size = byte_size(archive) + byte_size(metadata)
        archive.unlink(missing_ok=True)
        metadata.unlink(missing_ok=True)
        return size

    def prune(self, view: Mapping[str, Any], current_digest: str) -> int:
        retained = 0
        released = 0
        for row in self.entries(view):
            if row["sha256"] == current_digest or retained >= keep_versions(view) - 1:
                released += self.forget(view, row["id"])
            else:
                retained += 1
        return released

    def remove(self, view: Mapping[str, Any]) -> None:
        folder = self.folder(view)
        if folder.exists():
            shutil.rmtree(folder)
