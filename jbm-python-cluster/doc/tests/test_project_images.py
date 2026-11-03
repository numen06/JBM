import io

import httpx
import pytest
from fastapi.testclient import TestClient
from PIL import Image

from jbm_cluster_py.platform.doc.main import create_app
from test_doc_compat import doc_config


def image_bytes(size=(1280,720)):
    buffer=io.BytesIO(); Image.new('RGB',size,(30,90,140)).save(buffer,format='WEBP')
    return buffer.getvalue()


@pytest.mark.parametrize('writer_id',[0,10])
def test_project_images_use_doc_storage_and_live_project_membership(tmp_path,monkeypatch,writer_id):
    original=httpx.AsyncClient
    members={'writer':{'11'},'reader':{'11'},'outsider':{'12'}}
    identities={name:{'userId':i,'tenantId':1,'roles':['tenant_admin'] if name=='writer' else [],'admin':False}
                for i,name in enumerate(members,10)}
    identities['writer']['userId']=writer_id
    outage=False
    def upstream(request):
        token=request.headers.get('authorization','').removeprefix('Bearer ')
        if request.url.path.endswith('/security/authorize'):
            if token not in identities:return httpx.Response(401,json={'success':False})
            return httpx.Response(200,json={'success':True,'result':{'allowed':True,'identity':identities[token]}})
        assert request.url.path.endswith('/project-scope')
        assert request.headers['x-tenant-id']=='1'
        if outage:return httpx.Response(500,json={'success':False})
        allowed=request.url.params['projectId'] in members.get(token,set())
        return httpx.Response(200 if allowed else 403,json={'success':allowed,'data':{'allowed':allowed}})
    monkeypatch.setattr(httpx,'AsyncClient',lambda *args,**kwargs:original(transport=httpx.MockTransport(upstream)))
    config=doc_config(tmp_path); config.raw['jbm']['api-access']['enabled']=True
    config.raw['jbm']['doc']={'project-scope-url':'http://building/api/v1/building/project-scope'}
    def headers(name,project='11'):
        return {'Authorization':'Bearer '+name,'X-Tenant-Id':'1','X-Project-Id':project}
    with TestClient(create_app(config)) as client:
        root='/project-images/1/11'
        assert client.post('/upload?group=project-images/1/11',headers=headers('reader'),files={'file':('cover.webp',image_bytes(),'image/webp')}).status_code==403
        assert client.post(root,headers=headers('reader'),files={'file':('cover.webp',image_bytes(),'image/webp')}).status_code==403
        assert client.post(root,headers=headers('writer'),files={'file':('small.webp',image_bytes((320,180)),'image/webp')}).status_code==400
        uploaded=client.post(root,headers=headers('writer'),files={'file':('cover.webp',image_bytes(),'image/webp')})
        assert uploaded.status_code==200
        value=uploaded.json()['result']; assert value['width']==1280 and value['height']==720
        import asyncio
        metadata=asyncio.run(client.app.state.doc_service.repository.get_doc_by_id(value['docId']))
        assert str(metadata['creator'])==str(identities['writer']['userId'])  # Compatible with the existing MySQL BIGINT column.
        path=value['url'].removeprefix('/jbm-api/doc')
        assert client.get(path).status_code==401
        response=client.get(path,headers=headers('reader'))
        assert response.status_code==200 and response.content==image_bytes()
        assert response.headers['cache-control']=='private, no-store'
        assert client.get(path,headers=headers('outsider')).status_code==403
        assert client.get(path.replace('/1/11/','/1/12/'),headers=headers('outsider','12')).status_code==404
        assert client.get(path.replace('/1/11/','/2/11/'),headers=headers('reader')).status_code==403
        members['writer'].clear()
        assert client.get(path,headers=headers('writer')).status_code==403
        legacy='/get/project-images/1/11/'+value['docId']+'.webp'
        assert client.get(legacy,headers=headers('writer')).status_code==403
        outage=True
        assert client.get(path,headers=headers('reader')).status_code==503
