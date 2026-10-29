"""Apply only session-policy guards to the pinned, deployed service image."""
import hashlib
import json
from pathlib import Path

SOURCE = '/app/center/src/jbm_cluster_py/platform/center/modules/governance/application/request_access.py'
REPLACEMENTS = [('            return await self.signed(body, api)\n'
  '        identity = await self.auth.authenticate(authorization)\n'
  '        require_api_boundary(service, path, identity)\n'
  '        if api is not None:\n'
  '            if api.get("status") != 1:\n'
  '                raise HTTPException(403, "接口已停用")\n',
  '            return await self.signed(body, api)\n'
  '        identity = await self.auth.authenticate(authorization)\n'
  '        require_api_boundary(service, path, identity)\n'
  '        self.require_recent_authentication(service, method, path, identity)\n'
  '        if api is not None:\n'
  '            if api.get("status") != 1:\n'
  '                raise HTTPException(403, "接口已停用")\n'),
 ('        else:\n'
  '            authorize_api(service, method, path, identity)\n'
  '        return {"allowed": True, "identity": identity}\n'
  '\n'
  '    async def signed(self, body: dict[str, Any], api: dict[str, Any] | None) -> dict[str, Any]:\n'
  '        if api is None or api.get("status") != 1 or api.get("is_open") != 1:\n',
  '        else:\n'
  '            authorize_api(service, method, path, identity)\n'
  '        return {"allowed": True, "identity": identity}\n'
  '\n'
  '    @staticmethod\n'
  '    def require_recent_authentication(service: str, method: str, path: str, identity: dict[str, Any]) -> '
  'None:\n'
  '        if method in {"GET", "HEAD", "OPTIONS"} or path.rsplit("/", 1)[-1].lower() in {"pagelist", '
  '"list", "tree", "model", "root", "check", "query"}:\n'
  '            return\n'
  '        sensitive = bool(\n'
  '            ("center" in service and '
  're.search(r"/(user|baseUser|account|baseAccount|role|baseRole|authority|baseAuthority|grant|delegations|baseTenantApp)(/|$)", '
  'path))\n'
  '            or re.search(r"/(batch[^/]*|bulk[^/]*)(/|$)", path, re.I)\n'
  '            or re.search(r"/air-conditioning/automation/(scenes|policies)/[^/]+/(execute|run)$", path)\n'
  '        )\n'
  '        if sensitive and identity.get("userId") is not None:\n'
  '            auth_time = int(identity.get("authTime") or 0)\n'
  '            if not auth_time or time.time() - auth_time >= int(identity.get("recentAuthSeconds") or '
  '600):\n'
  '                raise HTTPException(401, "此敏感操作需要重新验证身份，请重新登录后再试")\n'
  '\n'
  '    async def signed(self, body: dict[str, Any], api: dict[str, Any] | None) -> dict[str, Any]:\n'
  '        if api is None or api.get("status") != 1 or api.get("is_open") != 1:\n')]

def patch(content):
    for before, after in REPLACEMENTS:
        assert content.count(before) == 1, 'Unexpected service base: ' + SOURCE
        content = content.replace(before, after, 1)
    return content

if __name__ == '__main__':
    path = Path(SOURCE)
    content = patch(path.read_text(encoding='utf-8'))
    compile(content, SOURCE, 'exec')
    path.write_text(content, encoding='utf-8')
    print(json.dumps({SOURCE: hashlib.sha256(content.encode()).hexdigest()}))
