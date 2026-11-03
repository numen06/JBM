"""Project display images use Doc storage and the project's live access boundary."""
from __future__ import annotations

import io
import os
import logging
from typing import Any

import httpx
from fastapi import APIRouter, File, HTTPException, Request, UploadFile
from fastapi.responses import StreamingResponse
from PIL import Image

from jbm_cluster_py.common.api_access import is_platform, permission_codes, role_codes
from jbm_cluster_py.common.result import ok

logger = logging.getLogger(__name__)


async def authorize_project(request: Request, tenant: int, project: int, config: Any) -> None:
    if request.headers.get('X-Tenant-Id') != str(tenant) or request.headers.get('X-Project-Id') != str(project):
        raise HTTPException(403, '图片所属租户或项目与请求范围不一致')
    url = os.getenv('JBM_DOC_PROJECT_SCOPE_URL') or config.get('jbm.doc.project-scope-url')
    if not url:
        instance = await request.app.state.project_discovery.choose_instance('smart-building-app')
        if not instance or not instance.get('ip') or not instance.get('port'):
            logger.warning('Project image access: smart-building-app has no available registered instance')
            raise HTTPException(503, '项目权限服务暂不可用')
        url = f'http://{instance["ip"]}:{int(instance["port"])}/api/v1/building/project-scope'
    try:
        async with httpx.AsyncClient(timeout=10, trust_env=False) as client:
            response = await client.get(url, params={'projectId':project}, headers={
                'Authorization':request.headers.get('Authorization',''),
                'X-Tenant-Id':str(tenant), 'X-Project-Id':str(project),
            })
        body = response.json()
    except (httpx.HTTPError, ValueError) as exc:
        logger.warning('Project image scope request failed: %s', type(exc).__name__)
        raise HTTPException(503, '项目权限服务暂不可用') from None
    if response.status_code in {401,403,404}:
        raise HTTPException(response.status_code, '当前账号无权访问该项目图片')
    if not isinstance(body,dict):
        raise HTTPException(503, '项目权限核对失败')
    value = body.get('result',body.get('data'))
    if response.status_code != 200 or body.get('success') is not True or not isinstance(value,dict) or value.get('allowed') is not True:
        raise HTTPException(503, '项目权限核对失败')


def build_project_image_router(service: Any, check_project=authorize_project) -> APIRouter:
    router=APIRouter(prefix='/project-images',tags=['项目图片'])

    @router.post('/{tenant}/{project}')
    async def upload(request: Request, tenant: int, project: int, file: UploadFile = File(...)):
        identity=getattr(request.state,'identity',{})
        if not (is_platform(identity) or role_codes(identity) & {'tenant_admin','iot_admin'} or
                'building.platform.operate' in permission_codes(identity)):
            raise HTTPException(403,'需要项目资料维护权限')
        await check_project(request,tenant,project,service.config)
        content=await file.read(1024*1024+1)
        if not content or len(content)>1024*1024:
            raise HTTPException(400,'项目图片不能为空或超过 1MB')
        try:
            with Image.open(io.BytesIO(content)) as image:
                extension={'PNG':'png','JPEG':'jpg','WEBP':'webp'}[image.format]
                width,height=image.size
                if not (640<=width<=1920 and 360<=height<=1080 and abs(width*9-height*16)<=16):
                    raise ValueError('尺寸不符合要求')
                image.load()
        except (OSError,ValueError,SyntaxError,KeyError,Image.DecompressionBombError):
            raise HTTPException(400,'请选择 640×360 至 1920×1080 的 16:9 PNG、JPG 或 WebP 图片') from None
        prepared=UploadFile(io.BytesIO(content),filename='project-image.'+extension,
            headers={'content-type':{'png':'image/png','jpg':'image/jpeg','webp':'image/webp'}[extension]})
        group=f'project-images/{tenant}/{project}'
        # Production's existing creator column is BIGINT. Project ownership is
        # enforced by the scoped group and live project check, not this field.
        creator=identity.get('userId')
        if creator is None:
            creator=identity.get('user_id')
        if creator is None:
            raise HTTPException(403,'图片上传缺少用户归属')
        doc=await service.upload_doc(prepared,group=group,creator=creator)
        return ok({'docId':doc['docId'],'url':f'/jbm-api/doc/project-images/{tenant}/{project}/{doc["docId"]}',
                   'width':width,'height':height,'size':len(content)},'图片上传成功')

    @router.get('/{tenant}/{project}/{doc_id}')
    async def read(request: Request, tenant: int, project: int, doc_id: str):
        await check_project(request,tenant,project,service.config)
        doc=await service.repository.get_doc_by_id(doc_id)
        if not doc or doc.get('docGroup')!=f'project-images/{tenant}/{project}':
            raise HTTPException(404,'项目图片不存在')
        _,obj=await service.get_doc(doc['docPath'])
        return StreamingResponse(obj.iter_chunks(),media_type=doc['contentType'],headers={
            'Content-Length':str(obj.size),'Cache-Control':'private, no-store','X-Content-Type-Options':'nosniff'})

    return router
