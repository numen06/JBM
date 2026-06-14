from __future__ import annotations

import base64
import hashlib
import hmac
import mimetypes
import uuid
from datetime import datetime, timedelta, timezone
from pathlib import PurePosixPath
from typing import Any, Dict, Iterable, List, Mapping, Optional
from urllib.parse import quote, urlencode

import httpx
from fastapi import UploadFile

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.common.masterdata import PageForm, first_payload, now_iso, page_form_from_body
from jbm_cluster_py.integrations.storage import StorageBackend, safe_object_key
from jbm_cluster_py.platform.doc.repository import DocRepository

TEXT_EDITABLE_EXTENSIONS = {
    ".bat",
    ".conf",
    ".csv",
    ".css",
    ".env",
    ".htm",
    ".html",
    ".ini",
    ".java",
    ".js",
    ".json",
    ".log",
    ".md",
    ".properties",
    ".py",
    ".sql",
    ".text",
    ".toml",
    ".ts",
    ".txt",
    ".vue",
    ".xml",
    ".yaml",
    ".yml",
}
TEXT_EDITABLE_TYPES = {
    "application/json",
    "application/xml",
    "application/x-ndjson",
    "application/yaml",
    "text/csv",
    "text/html",
    "text/markdown",
    "text/plain",
    "text/xml",
    "text/yaml",
}
MAX_TEXT_EDIT_BYTES = 1024 * 1024


def object_id() -> str:
    return uuid.uuid4().hex[:24]


def simple_uuid() -> str:
    return uuid.uuid4().hex


def normalize_path(*parts: Optional[str]) -> str:
    joined = "/".join(str(part or "").strip("/") for part in parts if str(part or "").strip("/"))
    return safe_object_key(str(PurePosixPath("/" + joined))[1:])


def version_payload(version_no: int = 1) -> Dict[str, Any]:
    return {"major": version_no, "minor": 0, "patch": 0, "value": str(version_no)}


class DocService:
    def __init__(self, config: AppConfig, repository: DocRepository, storage: StorageBackend) -> None:
        self.config = config
        self.repository = repository
        self.storage = storage

    async def start(self) -> None:
        await self.repository.start()
        await self.storage.start()

    async def stop(self) -> None:
        await self.storage.stop()
        await self.repository.stop()

    async def upload_doc(
        self,
        file: UploadFile,
        *,
        group: Optional[str] = None,
        group_id: Optional[str] = None,
        request_path: str = "",
        creator: Optional[str] = None,
    ) -> Dict[str, Any]:
        if file is None:
            raise ValueError("上传文件为空")
        body = await file.read()
        if body is None:
            raise ValueError("上传文件为空")
        doc_id = object_id()
        filename = file.filename or "%s.bin" % doc_id
        extension = PurePosixPath(filename).suffix.lstrip(".") or "bin"
        stored_name = "%s.%s" % (doc_id, extension)
        doc_path = normalize_path(group, request_path, stored_name)
        content_type = file.content_type or mimetypes.guess_type(filename)[0] or "application/octet-stream"
        await self.storage.put_bytes(doc_path, body, content_type)
        doc = {
            "docId": doc_id,
            "docName": filename,
            "size": len(body),
            "docGroupId": group_id,
            "docGroup": group,
            "docPath": doc_path,
            "state": "ACTIVE",
            "contentType": content_type,
            "version": version_payload(1),
            "creator": creator,
        }
        saved = await self.repository.save_doc(doc)
        await self._save_version(saved, doc_path, 1, creator)
        return saved

    async def get_doc(self, file_path: str, group: Optional[str] = None) -> tuple[Dict[str, Any], Any]:
        doc_path = normalize_path(group, file_path)
        doc = await self.repository.get_doc_by_path(doc_path)
        storage_object = await self.storage.get_object(doc_path)
        if doc is None:
            doc_id = PurePosixPath(doc_path).stem
            doc = await self.repository.save_doc(
                {
                    "docId": doc_id,
                    "docName": PurePosixPath(doc_path).name,
                    "size": storage_object.size,
                    "docPath": doc_path,
                    "state": "ACTIVE",
                    "contentType": storage_object.content_type,
                    "version": version_payload(1),
                }
            )
        doc["size"] = doc.get("size") or storage_object.size
        doc["contentType"] = doc.get("contentType") or storage_object.content_type
        return doc, storage_object

    async def remove_doc(self, file_path: str, group: Optional[str] = None) -> bool:
        doc_path = normalize_path(group, file_path)
        await self.storage.delete(doc_path)
        doc = await self.repository.get_doc_by_path(doc_path)
        if doc:
            await self.repository.delete_docs_by_ids([doc["docId"]])
        else:
            await self.repository.delete_docs_by_paths([doc_path])
        return True

    async def page_docs(self, body: Optional[Mapping[str, Any]]) -> Dict[str, Any]:
        payload = first_payload(body, ["baseDoc", "doc", "entity"])
        return await self.repository.page_docs(payload, page_form_from_body(body))

    async def sync_storage_docs(self, prefix: Optional[str] = None) -> Dict[str, Any]:
        entries = await self.storage.list_objects(normalize_path(prefix) if prefix else "")
        summary = {"scanned": 0, "created": 0, "skipped": 0, "failed": 0, **self.storage.describe()}
        existing_paths = await self.repository.existing_doc_paths()
        existing_ids = await self.repository.existing_doc_ids()
        seen_ids = set(existing_ids)
        docs_to_create: List[Dict[str, Any]] = []
        for entry in entries:
            summary["scanned"] += 1
            try:
                doc_path = normalize_path(entry.key)
                if not doc_path:
                    summary["skipped"] += 1
                    continue
                if doc_path in existing_paths:
                    summary["skipped"] += 1
                    continue
                path = PurePosixPath(doc_path)
                doc_id = path.stem or object_id()
                while doc_id in seen_ids:
                    doc_id = object_id()
                seen_ids.add(doc_id)
                content_type = entry.content_type or mimetypes.guess_type(path.name)[0] or "application/octet-stream"
                docs_to_create.append(
                    {
                        "docId": doc_id,
                        "docName": path.name or doc_path,
                        "size": entry.size,
                        "docPath": doc_path,
                        "docGroup": str(path.parent) if str(path.parent) != "." else None,
                        "state": "ACTIVE",
                        "contentType": content_type,
                        "version": version_payload(1),
                        "createTime": now_iso(),
                        "updateTime": now_iso(),
                    }
                )
            except Exception:
                summary["failed"] += 1
        if docs_to_create:
            try:
                summary["created"] = await self.repository.insert_docs(docs_to_create)
            except Exception:
                summary["failed"] += len(docs_to_create)
                raise
        return summary

    async def storage_status(self) -> Dict[str, Any]:
        return self.storage.describe()

    async def get_text_doc(self, body: Optional[Mapping[str, Any]]) -> Dict[str, Any]:
        doc_path = self._doc_path_from_body(body)
        doc, storage_object = await self.get_doc(doc_path)
        if not self.is_text_editable(doc):
            raise ValueError("当前文件类型不支持文本编辑")
        if storage_object.size > MAX_TEXT_EDIT_BYTES:
            raise ValueError("文本文件超过 1MB，暂不支持在线编辑")
        content = storage_object.body.decode("utf-8-sig")
        return {
            "doc": doc,
            "content": content,
            "encoding": "utf-8",
            "editable": True,
            "maxSize": MAX_TEXT_EDIT_BYTES,
        }

    async def save_text_doc(self, body: Optional[Mapping[str, Any]]) -> Dict[str, Any]:
        payload = first_payload(body, ["baseDoc", "doc", "entity"])
        doc_path = self._doc_path_from_body(body)
        content = str(payload.get("content") if payload.get("content") is not None else (body or {}).get("content") or "")
        encoded = content.encode("utf-8")
        if len(encoded) > MAX_TEXT_EDIT_BYTES:
            raise ValueError("文本文件超过 1MB，暂不支持在线编辑")
        doc, _ = await self.get_doc(doc_path)
        if not self.is_text_editable(doc):
            raise ValueError("当前文件类型不支持文本编辑")
        content_type = str(doc.get("contentType") or "text/plain")
        if content_type == "application/octet-stream":
            content_type = mimetypes.guess_type(str(doc.get("docPath") or doc_path))[0] or "text/plain"
        await self.storage.put_bytes(str(doc["docPath"]), encoded, content_type)
        version_no = await self.repository.latest_version_no(str(doc["docId"])) + 1
        updated = {
            **doc,
            "size": len(encoded),
            "contentType": content_type,
            "version": version_payload(version_no),
        }
        await self.repository.save_doc(updated)
        await self._save_version(updated, str(doc["docPath"]), version_no, str(payload.get("modifier") or payload.get("creator") or ""))
        return updated

    def is_text_editable(self, doc: Mapping[str, Any]) -> bool:
        content_type = str(doc.get("contentType") or "").split(";")[0].strip().lower()
        doc_path = str(doc.get("docPath") or doc.get("docName") or "")
        extension = PurePosixPath(doc_path).suffix.lower()
        return content_type.startswith("text/") or content_type in TEXT_EDITABLE_TYPES or extension in TEXT_EDITABLE_EXTENSIONS

    def _doc_path_from_body(self, body: Optional[Mapping[str, Any]]) -> str:
        payload = first_payload(body, ["baseDoc", "doc", "entity"])
        doc_path = str(payload.get("docPath") or (body or {}).get("docPath") or "")
        if not doc_path and payload.get("docId"):
            raise ValueError("请传入 docPath")
        if not doc_path:
            raise ValueError("请传入 docPath")
        return normalize_path(doc_path)

    async def delete_docs_by_ids(self, ids: Iterable[str]) -> bool:
        for doc_id in ids:
            doc = await self.repository.get_doc_by_id(str(doc_id))
            if doc and doc.get("docPath"):
                await self.storage.delete(str(doc["docPath"]))
        return await self.repository.delete_docs_by_ids([str(value) for value in ids])

    async def delete_docs_by_paths(self, paths: Iterable[str]) -> bool:
        safe_paths = [normalize_path(path) for path in paths]
        for path in safe_paths:
            await self.storage.delete(path)
        return await self.repository.delete_docs_by_paths(safe_paths)

    async def create_temp_group(self, group: Optional[Mapping[str, Any]]) -> Dict[str, Any]:
        data = dict(group or {})
        group_id = str(data.get("groupId") or simple_uuid())
        group_path = str(data.get("groupPath") or simple_uuid())
        saved_group = await self.repository.save_group(
            {
                "groupId": group_id,
                "groupPath": group_path,
                "expirationTime": data.get("expirationTime"),
                "autoClear": data.get("autoClear"),
                "maxQuantity": data.get("maxQuantity"),
                "docGroupName": data.get("docGroupName"),
            }
        )
        token = await self.create_group_token(saved_group.get("expirationTime"), group_id)
        saved_group["tokenKey"] = token["tokenKey"]
        await self.repository.save_group(saved_group)
        return saved_group

    async def create_group_token(self, expiration_time: Optional[str], group_id: str) -> Dict[str, Any]:
        if not expiration_time:
            expiration_time = (datetime.now(timezone.utc) + timedelta(days=1)).isoformat()
        return await self.repository.save_token(
            {
                "tokenKey": simple_uuid(),
                "expirationTime": expiration_time,
                "effectiveTime": 1,
                "effectiveTimeType": 2,
                "docGroupId": group_id,
            }
        )

    async def check_token(self, token_key: str) -> Optional[Dict[str, Any]]:
        token = await self.repository.get_token(token_key)
        if not token:
            return None
        expiration = token.get("expirationTime")
        if expiration:
            try:
                parsed = datetime.fromisoformat(str(expiration).replace("Z", "+00:00"))
                if parsed.tzinfo is None:
                    parsed = parsed.replace(tzinfo=timezone.utc)
                if parsed < datetime.now(timezone.utc):
                    return None
            except ValueError:
                pass
        return token

    async def find_group_items_by_token(self, token_key: str) -> List[Dict[str, Any]]:
        token = await self.check_token(token_key)
        if token is None:
            raise ValueError("文档token失效或者无效")
        group = await self.repository.get_group_by_id(str(token.get("docGroupId") or ""))
        if group is None:
            raise ValueError("没有找到对应的分组")
        return await self.repository.list_docs({"docGroup": group.get("groupPath")}, 100)

    async def remove_group_items_by_token(self, token_key: str, ids: Iterable[str], paths: Iterable[str]) -> bool:
        token = await self.check_token(token_key)
        if token is None:
            raise ValueError("文档token失效或者无效")
        if list(paths):
            return await self.delete_docs_by_paths(paths)
        if list(ids):
            return await self.delete_docs_by_ids(ids)
        raise ValueError("没有填写任何参数")

    async def upload_by_token(self, token_key: str, file: UploadFile) -> str:
        token = await self.check_token(token_key)
        if token is None:
            raise ValueError("文档token失效或者无效")
        group = await self.repository.get_group_by_id(str(token.get("docGroupId") or ""))
        if group is None:
            raise ValueError("没有找到对应的分组")
        doc = await self.upload_doc(
            file,
            group=group.get("groupPath"),
            group_id=group.get("groupId"),
            request_path="uploadByToken",
        )
        return str(doc["docPath"])

    async def master_page(self, entity: str, body: Optional[Mapping[str, Any]]) -> Dict[str, Any]:
        payload = first_payload(body, [entity, "entity"])
        page_form = page_form_from_body(body)
        if entity == "baseDocGroup":
            return await self.repository.page_groups(payload, page_form)
        if entity == "baseDocToken":
            return await self.repository.page_tokens(payload, page_form)
        return await self.repository.page_docs(payload, page_form)

    async def master_list(self, entity: str, body: Optional[Mapping[str, Any]]) -> List[Dict[str, Any]]:
        page = await self.master_page(entity, {**dict(body or {}), "pageForm": {"currPage": 1, "pageSize": 100}})
        return list(page["contents"])

    async def master_model(self, entity: str, body: Optional[Mapping[str, Any]]) -> Optional[Dict[str, Any]]:
        payload = first_payload(body, [entity, "entity"])
        if entity == "baseDocGroup":
            return await self.repository.get_group_by_id(str(payload.get("groupId") or ""))
        if entity == "baseDocToken":
            return await self.repository.get_token(str(payload.get("tokenKey") or ""))
        return await self.repository.get_doc_by_id(str(payload.get("docId") or ""))

    async def master_save(self, entity: str, body: Optional[Mapping[str, Any]]) -> Dict[str, Any]:
        payload = first_payload(body, [entity, "entity"])
        if entity == "baseDocGroup":
            payload.setdefault("groupId", simple_uuid())
            payload.setdefault("groupPath", simple_uuid())
            return await self.repository.save_group(payload)
        if entity == "baseDocToken":
            payload.setdefault("tokenKey", simple_uuid())
            payload.setdefault("expirationTime", (datetime.now(timezone.utc) + timedelta(days=1)).isoformat())
            return await self.repository.save_token(payload)
        payload.setdefault("docId", object_id())
        return await self.repository.save_doc(payload)

    async def master_save_batch(self, entity: str, body: Optional[Mapping[str, Any]]) -> List[Dict[str, Any]]:
        rows = (body or {}).get(entity) or (body or {}).get("list") or (body or {}).get("entitys") or []
        result = []
        for row in rows if isinstance(rows, list) else []:
            result.append(await self.master_save(entity, {entity: row}))
        return result

    async def master_delete(self, entity: str, body: Optional[Mapping[str, Any]]) -> bool:
        payload = first_payload(body, [entity, "entity"])
        if entity == "baseDocGroup":
            return await self.repository.delete_groups_by_ids([str(payload.get("groupId") or "")])
        if entity == "baseDocToken":
            return await self.repository.delete_tokens_by_ids([str(payload.get("tokenKey") or "")])
        return await self.delete_docs_by_ids([str(payload.get("docId") or "")])

    async def master_delete_by_ids(self, entity: str, ids: Iterable[Any]) -> bool:
        values = [str(value) for value in ids]
        if entity == "baseDocGroup":
            return await self.repository.delete_groups_by_ids(values)
        if entity == "baseDocToken":
            return await self.repository.delete_tokens_by_ids(values)
        return await self.delete_docs_by_ids(values)

    async def get_view_url(self, file_url: str, check_token: bool = True) -> Dict[str, Any]:
        file_type = PurePosixPath(file_url).suffix.lstrip(".") or "doc"
        token = simple_uuid()
        params = {
            "_w_appid": str(self.config.wps.get("appid") or ""),
        }
        if check_token:
            params["_w_tokentype"] = "1"
        redirect_key = str(self.config.redirect.get("key") or "_w_redirect")
        params[redirect_key] = str(self.config.redirect.get("value") or "")
        params["_w_userid"] = "-1"
        params["_w_filepath"] = file_url
        params["_w_filetype"] = "web"
        return {"expires_in": 600, "token": token, "wpsUrl": self._wps_url(params, file_type, token)}

    async def get_file_info(self, user_id: str, file_path: str, file_type: str) -> Dict[str, Any]:
        if (file_type or "web").lower() == "web":
            size = await self._remote_file_size(file_path)
            name = PurePosixPath(file_path).name or file_path
            return {
                "file": {
                    "id": hashlib.md5(file_path.encode("utf-8")).hexdigest(),
                    "name": name,
                    "version": 1,
                    "size": size,
                    "creator": user_id or "-1",
                    "create_time": int(datetime.now(timezone.utc).timestamp()),
                    "download_url": file_path,
                    "user_acl": {"rename": 0, "history": 1},
                    "watermark": {
                        "type": 0,
                        "value": "",
                        "fillstyle": "rgba( 192, 192, 192, 0.6 )",
                        "font": "bold 20px Serif",
                        "rotate": 0,
                        "horizontal": 50,
                        "vertical": 50,
                    },
                },
                "user": {
                    "id": user_id or "-1",
                    "name": "我",
                    "permission": "read",
                    "avatar_url": "https://zmfiletest.oss-cn-hangzhou.aliyuncs.com/user0.png",
                },
            }
        doc = await self.repository.get_doc_by_id(file_path) or await self.repository.get_doc_by_path(file_path)
        if not doc:
            raise ValueError("文件不存在")
        return {"file": self._doc_to_wps_file(doc), "user": {"id": user_id or "-1", "name": "我", "permission": "write"}}

    async def wps_save(self, file: UploadFile, user_id: str, doc_id: Optional[str]) -> Dict[str, Any]:
        if not doc_id:
            raise ValueError("缺少文件ID")
        doc = await self.repository.get_doc_by_id(doc_id)
        if doc is None:
            raise ValueError("文件不存在")
        body = await file.read()
        version_no = await self.repository.latest_version_no(doc_id) + 1
        object_key = normalize_path("versions", doc_id, "%s-%s" % (version_no, doc["docName"]))
        content_type = file.content_type or doc.get("contentType") or "application/octet-stream"
        await self.storage.put_bytes(object_key, body, content_type)
        updated = {
            **doc,
            "docName": file.filename or doc.get("docName"),
            "size": len(body),
            "contentType": content_type,
            "version": version_payload(version_no),
            "creator": user_id or doc.get("creator"),
        }
        await self.storage.put_bytes(str(doc["docPath"]), body, content_type)
        await self.repository.save_doc(updated)
        return await self._save_version(updated, object_key, version_no, user_id)

    async def wps_version(self, version_no: int, doc_id: Optional[str] = None) -> Dict[str, Any]:
        if not doc_id:
            return {}
        version = await self.repository.version(doc_id, version_no)
        return version or {}

    async def wps_rename(self, name: str, doc_id: Optional[str], user_id: str) -> bool:
        if not doc_id:
            raise ValueError("缺少文件ID")
        doc = await self.repository.get_doc_by_id(doc_id)
        if doc is None:
            raise ValueError("文件不存在")
        doc["docName"] = name
        doc["creator"] = user_id or doc.get("creator")
        await self.repository.save_doc(doc)
        return True

    async def wps_history(self, doc_id: Optional[str], offset: int = 0, count: int = 10) -> Dict[str, Any]:
        if not doc_id:
            return {"histories": [], "total": 0}
        rows = await self.repository.versions(doc_id, offset, count)
        return {"histories": rows, "total": len(rows)}

    async def wps_new(self, file: UploadFile, user_id: str) -> Dict[str, Any]:
        doc = await self.upload_doc(file, request_path="wps-new", creator=user_id)
        return self._doc_to_wps_file(doc)

    async def _save_version(self, doc: Mapping[str, Any], object_key: str, version_no: int, modifier: Optional[str]) -> Dict[str, Any]:
        version = {
            "id": "%s-%s" % (doc.get("docId"), version_no),
            "docId": doc.get("docId"),
            "version": version_no,
            "objectKey": object_key,
            "docName": doc.get("docName"),
            "size": doc.get("size"),
            "contentType": doc.get("contentType"),
            "modifier": modifier,
            "createTime": now_iso(),
        }
        return await self.repository.save_version(version)

    def _doc_to_wps_file(self, doc: Mapping[str, Any]) -> Dict[str, Any]:
        version = doc.get("version") or {}
        if isinstance(version, Mapping):
            version_no = int(version.get("major") or version.get("value") or 1)
        else:
            version_no = 1
        download_host = str(self.config.wps.get("downloadHost") or self.config.wps.get("download-host") or "").rstrip("/")
        download_url = "%s/download/%s" % (download_host, quote(str(doc.get("docPath") or ""))) if download_host else str(doc.get("docPath") or "")
        return {
            "id": doc.get("docId"),
            "name": doc.get("docName"),
            "version": version_no,
            "size": doc.get("size") or 0,
            "creator": str(doc.get("creator") or "-1"),
            "create_time": int(datetime.now(timezone.utc).timestamp()),
            "download_url": download_url,
            "user_acl": {"rename": 1, "history": 1},
            "watermark": {"type": 0, "value": ""},
        }

    def _wps_url(self, params: Mapping[str, str], file_type: str, file_id: str) -> str:
        domain = str(self.config.wps.get("domain") or "").rstrip("/") + "/"
        file_type_code = self._file_type_code(file_type)
        key_value = "".join("%s=%s&" % (key, value) for key, value in params.items())
        signature = self._wps_signature(params)
        return "%s%s/%s?%s_w_signature=%s" % (domain, file_type_code, file_id, key_value, signature)

    def _wps_signature(self, params: Mapping[str, str]) -> str:
        appsecret = str(self.config.wps.get("appsecret") or "")
        content = "".join("%s=%s" % (key, params[key]) for key in sorted(params) if key != "_w_signature")
        content += "_w_secretkey=%s" % appsecret
        digest = hmac.new(appsecret.encode("utf-8"), content.encode("utf-8"), hashlib.sha1).digest()
        return quote(base64.b64encode(digest).decode("ascii"), safe="")

    def _file_type_code(self, file_type: str) -> str:
        value = (file_type or "").lower().strip(".")
        if value in {"doc", "docx", "wps", "word"}:
            return "w"
        if value in {"xls", "xlsx", "et", "excel"}:
            return "s"
        if value in {"ppt", "pptx", "dps"}:
            return "p"
        if value in {"pdf"}:
            return "f"
        return "w"

    async def _remote_file_size(self, file_url: str) -> int:
        try:
            async with httpx.AsyncClient(timeout=5) as client:
                response = await client.head(file_url)
            return int(response.headers.get("content-length") or 0)
        except Exception:
            return 0
