from __future__ import annotations

from pathlib import Path
import json

from fastapi.testclient import TestClient
from sqlalchemy import text

from jbm_cluster_py.common.config import AppConfig
from jbm_cluster_py.platform.doc.main import create_app


def doc_config(tmp_path: Path) -> AppConfig:
    return AppConfig(
        {
            # Domain/storage tests; API authentication is exercised separately by framework tests.
            "jbm": {"api-access": {"enabled": False}},
            "server": {"host": "127.0.0.1", "port": 9999},
            "spring": {
                "application": {"name": "jbm-cluster-platform-doc"},
                "cloud": {"nacos": {"discovery": {"enabled": False}}},
            },
            "integrations": {
                "database": {"url": "sqlite+aiosqlite:///%s" % (tmp_path / "doc.db")},
                "storage": {"backend": "filesystem", "local-dir": str(tmp_path / "files")},
                "minio": {"enabled": False},
            },
            "wps": {
                "domain": "https://wwo.wps.cn/office/",
                "appid": "appid",
                "appsecret": "secret",
                "downloadHost": "http://doc.local",
            },
            "redirect": {"key": "_w_redirect", "value": "http://doc.local/callback"},
        },
        profile="test",
        config_dir=None,
        app="doc",
    )


def test_doc_upload_download_page_and_delete(tmp_path: Path) -> None:
    with TestClient(create_app(doc_config(tmp_path))) as client:
        uploaded = client.post("/upload?group=demo", files={"file": ("hello.txt", b"hello", "text/plain")})
        assert uploaded.status_code == 200
        doc_path = uploaded.json()["result"]

        page = client.post("/baseDoc/pageList", json={"pageForm": {"currPage": 1, "pageSize": 10}})
        rows = page.json()["result"]["contents"]
        assert rows[0]["docPath"] == doc_path

        inline = client.get("/get/%s" % doc_path)
        assert inline.status_code == 200
        assert inline.content == b"hello"
        assert inline.headers["content-disposition"].startswith("inline")

        downloaded = client.get("/download/%s" % doc_path)
        assert downloaded.status_code == 200
        assert downloaded.content == b"hello"
        assert downloaded.headers["content-disposition"].startswith("attachment")

        deleted = client.post("/baseDoc/deleteByPaths", json={"paths": [doc_path]})
        assert deleted.json()["success"] is True


def test_only_configured_raster_login_images_are_public(tmp_path: Path) -> None:
    config = doc_config(tmp_path)
    config.raw["jbm"]["api-access"]["enabled"] = True
    files = tmp_path / "files" / "app-branding" / "upload"
    files.mkdir(parents=True)
    png = b"\x89PNG\r\n\x1a\npublic-login-image"
    (files / "background.png").write_bytes(png)
    (files / "private.png").write_bytes(png)
    (files / "logo.svg").write_bytes(b'<svg xmlns="http://www.w3.org/2000/svg"/>')
    with TestClient(create_app(config)) as client:
        async def seed():
            async with client.app.state.doc_service.repository.engine.begin() as conn:
                await conn.execute(text("CREATE TABLE base_app_config (config_content TEXT)"))
                await conn.execute(text("INSERT INTO base_app_config VALUES (:value)"), {
                    "value": json.dumps({"sysBg": "app-branding/upload/background.png",
                                         "sysLogo": "app-branding/upload/logo.svg"})
                })
        client.portal.call(seed)
        image = client.get("/get/app-branding/upload/background.png")
        assert image.status_code == 200 and image.content == png
        assert image.headers["content-type"] == "image/png"
        assert image.headers["x-content-type-options"] == "nosniff"
        for path in ("/get/app-branding/upload/private.png", "/get/app-branding/upload/logo.svg",
                     "/download/app-branding/upload/background.png", "/get/private/report.pdf",
                     "/get/app-branding/%2e%2e/private/report.png", "/docs", "/openapi.json"):
            assert client.get(path).status_code == 401, path
        assert client.post("/get/app-branding/upload/background.png").status_code == 401


def test_doc_group_token_upload_and_query(tmp_path: Path) -> None:
    with TestClient(create_app(doc_config(tmp_path))) as client:
        group = client.post("/baseDocGroup/createTempGroup", json={"docGroupName": "临时材料"}).json()["result"]
        token = group["tokenKey"]

        uploaded = client.post(
            "/baseDocGroup/uploadByToken",
            headers={"Doc-Token-Key": token},
            files={"file": ("group.txt", b"group-file", "text/plain")},
        )
        assert uploaded.status_code == 200
        assert uploaded.json()["result"].endswith(".txt")

        rows = client.post("/baseDocGroup/findGroupItemByToken", headers={"Doc-Token-Key": token}, json={})
        assert rows.status_code == 200
        assert rows.json()["result"][0]["docGroupId"] == group["groupId"]


def test_doc_masterdata_and_wps_url(tmp_path: Path) -> None:
    with TestClient(create_app(doc_config(tmp_path))) as client:
        saved = client.post(
            "/baseDocToken/save",
            json={"baseDocToken": {"docId": "doc-1", "effectiveTime": 1}},
        )
        assert saved.status_code == 200
        token_key = saved.json()["result"]["tokenKey"]

        model = client.post("/baseDocToken/model", json={"baseDocToken": {"tokenKey": token_key}})
        assert model.json()["result"]["tokenKey"] == token_key

        view_url = client.get("/getViewUrl", params={"fileUrl": "http://example.com/demo.docx"})
        assert view_url.status_code == 200
        assert view_url.json()["result"]["wpsUrl"].startswith("https://wwo.wps.cn/office/w/")


def test_doc_sync_storage_imports_existing_files_once(tmp_path: Path) -> None:
    files_dir = tmp_path / "files"
    existing = files_dir / "legacy" / "manual.pdf"
    existing.parent.mkdir(parents=True)
    existing.write_bytes(b"legacy-file")

    with TestClient(create_app(doc_config(tmp_path))) as client:
        synced = client.post("/baseDoc/syncStorage", json={})
        result = synced.json()["result"]
        assert result["scanned"] == 1
        assert result["created"] == 1
        assert result["skipped"] == 0
        assert result["failed"] == 0

        page = client.post("/baseDoc/pageList", json={"baseDoc": {"docName": "manual"}, "pageForm": {"currPage": 1, "pageSize": 10}})
        rows = page.json()["result"]["contents"]
        assert rows[0]["docPath"] == "legacy/manual.pdf"
        assert rows[0]["docName"] == "manual.pdf"
        assert rows[0]["docGroup"] == "legacy"

        saved = client.post(
            "/baseDoc/save",
            json={"baseDoc": {"docId": rows[0]["docId"], "docName": "合同留存.pdf", "state": "ARCHIVED"}},
        )
        assert saved.status_code == 200
        assert saved.json()["result"]["docName"] == "合同留存.pdf"

        repeated = client.post("/baseDoc/syncStorage", json={}).json()["result"]
        assert repeated["scanned"] == 1
        assert repeated["created"] == 0
        assert repeated["skipped"] == 1


def test_doc_text_get_missing_storage_returns_business_error(tmp_path: Path) -> None:
    with TestClient(create_app(doc_config(tmp_path))) as client:
        saved = client.post(
            "/baseDoc/save",
            json={
                "baseDoc": {
                    "docId": "missing123",
                    "docName": "ghost.txt",
                    "docPath": "missing/ghost.txt",
                    "contentType": "text/plain",
                    "state": "ACTIVE",
                }
            },
        )
        assert saved.status_code == 200

        loaded = client.post("/baseDoc/text/get", json={"baseDoc": {"docPath": "missing/ghost.txt"}})
        assert loaded.status_code == 400
        body = loaded.json()
        assert body["success"] is False
        assert body["code"] == 400
        assert "文件不存在" in body["message"]


def test_doc_text_file_can_be_edited(tmp_path: Path) -> None:
    with TestClient(create_app(doc_config(tmp_path))) as client:
        uploaded = client.post("/upload?group=text", files={"file": ("notes.txt", b"old text", "text/plain")})
        doc_path = uploaded.json()["result"]

        loaded = client.post("/baseDoc/text/get", json={"baseDoc": {"docPath": doc_path}})
        assert loaded.status_code == 200
        assert loaded.json()["result"]["content"] == "old text"

        saved = client.post("/baseDoc/text/save", json={"baseDoc": {"docPath": doc_path, "content": "new text"}})
        assert saved.status_code == 200
        assert saved.json()["result"]["size"] == len("new text")

        downloaded = client.get("/download/%s" % doc_path)
        assert downloaded.content == b"new text"
