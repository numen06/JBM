from __future__ import annotations

import asyncio
import logging
import mimetypes
from pathlib import Path, PurePosixPath
from typing import Any, AsyncIterator, List, Mapping, Optional

logger = logging.getLogger(__name__)


def safe_object_key(value: str) -> str:
    normalized = str(PurePosixPath("/" + (value or "").replace("\\", "/")))[1:]
    if normalized.startswith("../") or "/../" in normalized or normalized == "..":
        raise ValueError("invalid object path")
    return normalized.strip("/")


class StorageObject:
    def __init__(self, key: str, body: bytes, content_type: str = "application/octet-stream") -> None:
        self.key = key
        self.body = body
        self.content_type = content_type
        self.size = len(body)

    async def iter_chunks(self, chunk_size: int = 1024 * 1024) -> AsyncIterator[bytes]:
        for index in range(0, len(self.body), chunk_size):
            yield self.body[index : index + chunk_size]


class StorageEntry:
    def __init__(self, key: str, size: int = 0, content_type: Optional[str] = None) -> None:
        self.key = key
        self.size = size
        self.content_type = content_type or "application/octet-stream"


class StorageBackend:
    backend_name = "unknown"

    async def start(self) -> None:
        return None

    async def stop(self) -> None:
        return None

    async def put_bytes(self, key: str, body: bytes, content_type: str) -> None:
        raise NotImplementedError

    async def get_object(self, key: str) -> StorageObject:
        raise NotImplementedError

    async def delete(self, key: str) -> None:
        raise NotImplementedError

    async def list_objects(self, prefix: str = "") -> List[StorageEntry]:
        raise NotImplementedError

    def describe(self) -> dict[str, Any]:
        return {"backend": self.backend_name}


class FilesystemStorage(StorageBackend):
    backend_name = "filesystem"

    def __init__(self, base_dir: str | Path) -> None:
        self.base_dir = Path(base_dir)

    async def start(self) -> None:
        self.base_dir.mkdir(parents=True, exist_ok=True)

    def _path(self, key: str) -> Path:
        safe_key = safe_object_key(key)
        path = (self.base_dir / safe_key).resolve()
        root = self.base_dir.resolve()
        if root not in path.parents and path != root:
            raise ValueError("invalid object path")
        return path

    async def put_bytes(self, key: str, body: bytes, content_type: str) -> None:
        path = self._path(key)
        path.parent.mkdir(parents=True, exist_ok=True)
        await asyncio.to_thread(path.write_bytes, body)

    async def get_object(self, key: str) -> StorageObject:
        path = self._path(key)
        body = await asyncio.to_thread(path.read_bytes)
        content_type = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
        return StorageObject(key=safe_object_key(key), body=body, content_type=content_type)

    async def delete(self, key: str) -> None:
        path = self._path(key)
        if path.exists():
            await asyncio.to_thread(path.unlink)

    async def list_objects(self, prefix: str = "") -> List[StorageEntry]:
        root = self._path(prefix) if prefix else self.base_dir.resolve()
        if not root.exists():
            return []
        files = await asyncio.to_thread(lambda: [path for path in root.rglob("*") if path.is_file()])
        base = self.base_dir.resolve()
        entries = []
        for path in files:
            key = path.resolve().relative_to(base).as_posix()
            entries.append(
                StorageEntry(
                    key=key,
                    size=path.stat().st_size,
                    content_type=mimetypes.guess_type(path.name)[0] or "application/octet-stream",
                )
            )
        return sorted(entries, key=lambda item: item.key)

    def describe(self) -> dict[str, Any]:
        return {"backend": self.backend_name, "localDir": str(self.base_dir)}


class S3Storage(StorageBackend):
    backend_name = "s3"

    def __init__(self, config: Mapping[str, Any]) -> None:
        self.config = dict(config)
        self.bucket = str(self.config.get("bucket") or "doc")
        self._session: Optional[Any] = None
        self._client_cm: Optional[Any] = None
        self._client: Optional[Any] = None

    async def start(self) -> None:
        import aioboto3

        self._session = aioboto3.Session(
            aws_access_key_id=self.config.get("access-key") or self.config.get("access_key"),
            aws_secret_access_key=self.config.get("secret-key") or self.config.get("secret_key"),
            region_name=self.config.get("region") or "us-east-1",
        )
        self._client_cm = self._session.client(
            "s3",
            endpoint_url=self.config.get("endpoint-url") or self.config.get("endpoint_url"),
        )
        self._client = await self._client_cm.__aenter__()
        try:
            await self._client.head_bucket(Bucket=self.bucket)
        except Exception:
            await self._client.create_bucket(Bucket=self.bucket)

    async def stop(self) -> None:
        if self._client_cm is not None:
            await self._client_cm.__aexit__(None, None, None)
        self._client_cm = None
        self._client = None

    async def put_bytes(self, key: str, body: bytes, content_type: str) -> None:
        await self._client.put_object(
            Bucket=self.bucket,
            Key=safe_object_key(key),
            Body=body,
            ContentType=content_type or "application/octet-stream",
        )

    async def get_object(self, key: str) -> StorageObject:
        response = await self._client.get_object(Bucket=self.bucket, Key=safe_object_key(key))
        async with response["Body"] as stream:
            body = await stream.read()
        content_type = response.get("ContentType") or "application/octet-stream"
        return StorageObject(key=safe_object_key(key), body=body, content_type=content_type)

    async def delete(self, key: str) -> None:
        await self._client.delete_object(Bucket=self.bucket, Key=safe_object_key(key))

    async def list_objects(self, prefix: str = "") -> List[StorageEntry]:
        safe_prefix = safe_object_key(prefix)
        continuation_token: Optional[str] = None
        entries: List[StorageEntry] = []
        while True:
            params: dict[str, Any] = {"Bucket": self.bucket, "Prefix": safe_prefix}
            if continuation_token:
                params["ContinuationToken"] = continuation_token
            response = await self._client.list_objects_v2(**params)
            for item in response.get("Contents") or []:
                key = str(item.get("Key") or "")
                if not key or key.endswith("/"):
                    continue
                content_type = mimetypes.guess_type(key)[0] or "application/octet-stream"
                entries.append(StorageEntry(key=safe_object_key(key), size=int(item.get("Size") or 0), content_type=content_type))
            if not response.get("IsTruncated"):
                break
            continuation_token = response.get("NextContinuationToken")
            if not continuation_token:
                break
        return entries

    def describe(self) -> dict[str, Any]:
        return {
            "backend": self.backend_name,
            "endpointUrl": self.config.get("endpoint-url") or self.config.get("endpoint_url"),
            "bucket": self.bucket,
        }


def build_storage(storage_config: Mapping[str, Any], minio_config: Mapping[str, Any]) -> StorageBackend:
    backend = str(storage_config.get("backend") or "").lower()
    if backend in {"s3", "minio"} or minio_config.get("enabled"):
        try:
            return S3Storage(minio_config)
        except Exception as exc:
            logger.warning("S3 storage unavailable, falling back to filesystem: %s", exc)
    return FilesystemStorage(storage_config.get("local-dir") or storage_config.get("local_dir") or "./data/files")
