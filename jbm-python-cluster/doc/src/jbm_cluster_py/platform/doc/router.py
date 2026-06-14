from __future__ import annotations

from typing import Any, Dict, Optional
from urllib.parse import quote

from fastapi import APIRouter, Body, File, Header, Query, Request, UploadFile
from fastapi.responses import StreamingResponse

from jbm_cluster_py.common.result import fail, ok
from jbm_cluster_py.platform.doc.schemas import DocPathForm, FileReqBody
from jbm_cluster_py.platform.doc.service import DocService


def _content_disposition(disposition: str, filename: str) -> str:
    safe = quote(filename or "file")
    return "%s; filename*=UTF-8''%s" % (disposition, safe)


def build_doc_router(service: DocService) -> APIRouter:
    router = APIRouter()

    @router.post("/put", tags=["文档资源管理"])
    async def put(file: UploadFile = File(...)) -> Dict[str, Any]:
        doc = await service.upload_doc(file, request_path="put")
        return ok(doc["docPath"], "上传文档成功")

    @router.post("/upload", tags=["文档资源管理"])
    async def upload(file: UploadFile = File(...), group: Optional[str] = Query(default=None)) -> Dict[str, Any]:
        doc = await service.upload_doc(file, group=group, request_path="upload")
        return ok(doc["docPath"], "上传文档成功")

    @router.get("/remove/{file_path:path}", tags=["文档资源管理"])
    async def remove(file_path: str) -> Dict[str, Any]:
        await service.remove_doc(file_path)
        return ok(None, "删除文档成功")

    @router.get("/get/{file_path:path}", tags=["文档资源管理"])
    async def get_file(file_path: str) -> StreamingResponse:
        doc, storage_object = await service.get_doc(file_path)
        return StreamingResponse(
            storage_object.iter_chunks(),
            media_type=doc.get("contentType") or "application/octet-stream",
            headers={
                "Content-Length": str(doc.get("size") or storage_object.size),
                "Content-Disposition": _content_disposition("inline", str(doc.get("docName") or "file")),
            },
        )

    @router.get("/download/{file_path:path}", tags=["文档资源管理"])
    async def download(file_path: str) -> StreamingResponse:
        doc, storage_object = await service.get_doc(file_path)
        return StreamingResponse(
            storage_object.iter_chunks(),
            media_type=doc.get("contentType") or "application/octet-stream",
            headers={
                "Content-Length": str(doc.get("size") or storage_object.size),
                "Content-Disposition": _content_disposition("attachment", str(doc.get("docName") or "file")),
            },
        )

    @router.get("/getViewUrl", tags=["文档资源管理"])
    async def get_view_url(fileUrl: str = Query(...)) -> Dict[str, Any]:
        return ok(await service.get_view_url(fileUrl, True))

    @router.post("/baseDoc/pageList", tags=["文档管理开放接口"])
    async def base_doc_page_list(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok(await service.page_docs(body), "查询分页列表成功")

    @router.post("/baseDoc/save", tags=["文档管理开放接口"])
    async def base_doc_save(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok(await service.master_save("baseDoc", body), "保存对象成功")

    @router.post("/baseDoc/model", tags=["文档管理开放接口"])
    async def base_doc_model(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok(await service.master_model("baseDoc", body), "查询对象成功")

    @router.post("/baseDoc/syncStorage", tags=["文档管理开放接口"])
    async def base_doc_sync_storage(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        prefix = (body or {}).get("prefix")
        return ok(await service.sync_storage_docs(prefix), "同步留存文档成功")

    @router.get("/baseDoc/storageStatus", tags=["文档管理开放接口"])
    async def base_doc_storage_status() -> Dict[str, Any]:
        return ok(await service.storage_status(), "查询存储状态成功")

    @router.post("/baseDoc/text/get", tags=["文档管理开放接口"])
    async def base_doc_text_get(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok(await service.get_text_doc(body), "读取文本文件成功")

    @router.post("/baseDoc/text/save", tags=["文档管理开放接口"])
    async def base_doc_text_save(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok(await service.save_text_doc(body), "保存文本文件成功")

    @router.post("/baseDoc/deleteByIds", tags=["文档管理开放接口"])
    async def base_doc_delete_by_ids(form: DocPathForm) -> Dict[str, Any]:
        if not form.ids:
            return fail(False, "ID为空")
        return ok(await service.delete_docs_by_ids(form.ids), "批量删除文件成功")

    @router.post("/baseDoc/deleteByPaths", tags=["文档管理开放接口"])
    async def base_doc_delete_by_paths(form: DocPathForm) -> Dict[str, Any]:
        if not form.paths:
            return fail(False, "路径为空")
        return ok(await service.delete_docs_by_paths(form.paths), "批量删除文件成功")

    @router.post("/baseDocGroup/createTempGroup", tags=["文档分组管理开放接口"])
    async def create_temp_group(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok(await service.create_temp_group(body))

    @router.post("/baseDocGroup/findGroupItemByToken", tags=["文档分组管理开放接口"])
    async def find_group_item_by_token(
        doc_token_key: str = Header(alias="Doc-Token-Key"),
        body: Optional[Dict[str, Any]] = Body(default=None),
    ) -> Dict[str, Any]:
        return ok(await service.find_group_items_by_token(doc_token_key), "查询组内文件成功")

    @router.post("/baseDocGroup/removeGroupItemByToken", tags=["文档分组管理开放接口"])
    async def remove_group_item_by_token(
        form: DocPathForm,
        doc_token_key: str = Header(alias="Doc-Token-Key"),
    ) -> Dict[str, Any]:
        return ok(
            await service.remove_group_items_by_token(doc_token_key, form.ids, form.paths),
            "删除组内文件成功",
        )

    @router.post("/baseDocGroup/uploadByToken", tags=["文档分组管理开放接口"])
    async def upload_by_token(
        file: UploadFile = File(...),
        doc_token_key: str = Header(alias="Doc-Token-Key"),
    ) -> Dict[str, Any]:
        return ok(await service.upload_by_token(doc_token_key, file), "上传组文档成功")

    def install_masterdata_routes(prefix: str, entity: str, tag: str) -> None:
        @router.post("%s/pageList" % prefix, tags=[tag])
        async def page_list(body: Optional[Dict[str, Any]] = Body(default=None), _entity: str = entity) -> Dict[str, Any]:
            return ok(await service.master_page(_entity, body), "查询分页列表成功")

        @router.post("%s/list" % prefix, tags=[tag])
        async def list_rows(body: Optional[Dict[str, Any]] = Body(default=None), _entity: str = entity) -> Dict[str, Any]:
            return ok(await service.master_list(_entity, body), "查询列表成功")

        @router.post("%s/model" % prefix, tags=[tag])
        async def model(body: Optional[Dict[str, Any]] = Body(default=None), _entity: str = entity) -> Dict[str, Any]:
            return ok(await service.master_model(_entity, body), "查询对象成功")

        @router.post("%s/save" % prefix, tags=[tag])
        async def save(body: Optional[Dict[str, Any]] = Body(default=None), _entity: str = entity) -> Dict[str, Any]:
            return ok(await service.master_save(_entity, body), "保存对象成功")

        @router.post("%s/saveBatch" % prefix, tags=[tag])
        async def save_batch(body: Optional[Dict[str, Any]] = Body(default=None), _entity: str = entity) -> Dict[str, Any]:
            return ok(await service.master_save_batch(_entity, body), "保存对象成功")

        @router.post("%s/delete" % prefix, tags=[tag])
        async def delete(body: Optional[Dict[str, Any]] = Body(default=None), _entity: str = entity) -> Dict[str, Any]:
            return ok(await service.master_delete(_entity, body), "删除对象成功")

        @router.post("%s/deleteByIds" % prefix, tags=[tag])
        async def delete_by_ids(body: Optional[Dict[str, Any]] = Body(default=None), _entity: str = entity) -> Dict[str, Any]:
            ids = (body or {}).get("ids") or []
            if not ids:
                return fail(True, "ID为空")
            return ok(await service.master_delete_by_ids(_entity, ids), "批量成功刪除")

    install_masterdata_routes("/baseDocGroup", "baseDocGroup", "文档分组管理开放接口")
    install_masterdata_routes("/baseDocToken", "baseDocToken", "文档 Token")

    @router.get("/v1/3rd/file/info", tags=["WPS文件回调接口"])
    async def wps_file_info(
        _w_userid: str = Query(default="-1"),
        _w_filepath: str = Query(...),
        _w_filetype: str = Query(default="web"),
    ) -> Any:
        return await service.get_file_info(_w_userid, _w_filepath, _w_filetype)

    @router.post("/v1/3rd/file/online", tags=["WPS文件回调接口"])
    async def wps_online(body: Optional[Dict[str, Any]] = Body(default=None)) -> Dict[str, Any]:
        return ok()

    @router.post("/v1/3rd/file/save", tags=["WPS文件回调接口"])
    async def wps_save(
        request: Request,
        file: UploadFile = File(...),
        _w_userid: str = Query(default="-1"),
    ) -> Dict[str, Any]:
        doc_id = request.headers.get("x-weboffice-file-id") or request.query_params.get("docId")
        return ok(await service.wps_save(file, _w_userid, doc_id))

    @router.get("/v1/3rd/file/version/{version}", tags=["WPS文件回调接口"])
    async def wps_version(request: Request, version: int) -> Dict[str, Any]:
        doc_id = request.headers.get("x-weboffice-file-id") or request.query_params.get("docId")
        return ok(await service.wps_version(version, doc_id))

    @router.put("/v1/3rd/file/rename", tags=["WPS文件回调接口"])
    async def wps_rename(
        request: Request,
        form: FileReqBody,
        _w_userid: str = Query(default="-1"),
    ) -> Dict[str, Any]:
        doc_id = request.headers.get("x-weboffice-file-id") or request.query_params.get("docId") or form.id
        await service.wps_rename(form.name or "", doc_id, _w_userid)
        return ok()

    @router.post("/v1/3rd/file/history", tags=["WPS文件回调接口"])
    async def wps_history(request: Request, form: FileReqBody) -> Dict[str, Any]:
        doc_id = request.headers.get("x-weboffice-file-id") or request.query_params.get("docId") or form.id
        return ok(await service.wps_history(doc_id, form.offset, form.count))

    @router.post("/v1/3rd/file/new", tags=["WPS文件回调接口"])
    async def wps_new(file: UploadFile = File(...), _w_userid: str = Query(default="-1")) -> Dict[str, Any]:
        return ok(await service.wps_new(file, _w_userid))

    return router
