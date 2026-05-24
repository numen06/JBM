# 前后端升级代码审查与实测计划

> 执行方：Cursor  
> 生成时间：2026-05-24  
> 项目根目录：`D:\workspaces\JBM7`  
> 约束：请由 Cursor 执行本计划中的构建、启动、接口、前端页面实测；Codex 已按用户要求停止继续实测。

## 1. 本次升级范围判断

本次未提交改动集中在“组织结构 + 多租户/多组织数据范围 + 前端组织选择”：

- 前端：
  - `jbm-admin-vue/src/components/OrgTreeSelect.vue`
  - `jbm-admin-vue/src/composables/useOrgTree.ts`
  - `jbm-admin-vue/src/views/system/UserList.vue`
  - `jbm-admin-vue/src/views/system/AppList.vue`
  - `jbm-admin-vue/src/api/types.ts`
  - `jbm-admin-vue/src/api/user.ts`
- 后端：
  - `BaseApp` 新增 `orgId`
  - `BaseUserForm` 新增 `orgIds/companyIds/departmentIds`
  - `JbmLoginUser/LoginHelper/LoginPostProcessor` 增加组织上下文
  - `BaseUserOrg`、`BaseUserOrgMapper`、`BaseUserOrgService`、`BaseUserOrgServiceImpl`
  - `OrgDataScopeHelper`
  - `BaseUserBusinessImpl/BaseUserController/BaseUserMapper.xml`
  - Liquibase：`V13__org_and_app_org.sql`、`V14__org_legacy_column_patch.sql`

## 2. 代码审查重点

### 2.1 前端风险点

- `useOrgTree.ts` 当前文件存在明显乱码显示，例如 `orgOptionLabel` 的缩进符号、`orgLabel` 空值返回。请先用 Cursor 检查文件编码与实际页面渲染，避免下拉框出现乱码。
- `OrgTreeSelect.vue` 内部 `onMounted(loadOrgs)`，调用方 `UserList/AppList` 也调用 `loadOrgs()`。如果 `useOrgTree()` 不是共享状态，则每个组件会重复请求组织树；若影响性能，建议改为单例缓存或由父组件统一传入。
- `UserList.vue` 编辑用户时会读取 `getUserOrgs`，但新建用户时只调用 `createUser`，后端需确认 `addUser(form)` 后 `form.userId` 一定回填，否则 `createUser` 后的跨组织授权不会保存。
- `AppList.vue` 新增“所属组织必填”，需要确认历史应用 `orgId` 为空时页面编辑不会卡死；Liquibase 已回填 `org_id=1`，但老环境必须验证。

### 2.2 后端风险点

- `BaseUserController#createUser`：
  - 先 `baseUserBusiness.addUser(form)`，再判断 `form.getUserId()` 保存 `orgIds`。
  - 必测：`addUser` 是否把新用户 ID 写回到同一个 `form` 对象；否则新建用户的跨组织授权会丢失。
- `BaseUserOrgServiceImpl#saveUserOrgs`：
  - `Long.parseLong(orgId.trim())` 没有异常保护；前端正常传数字字符串没问题，但接口应测试非法 orgId 的返回。
  - 没有校验 `orgId` 是否存在；建议至少测试传不存在组织 ID 是否会写入脏授权。
- `OrgDataScopeHelper#applyUserQueryScope`：
  - 多组织授权时只设置 `companyIds`，不会再套用主部门 `departmentIds`，这意味着一旦用户有额外组织授权，主组织下可能从“部门及下级”扩大到“整个公司”。这可能是设计，也可能是权限扩大风险，必须确认。
  - 只有一个组织时会应用部门子树；多个组织时不应用部门子树。
- `BaseUserMapper.xml`：
  - `departmentId LIKE concat(#{form.departmentId}, '%')` 对数字 ID 字符串前缀匹配可能误伤，例如 `12` 会匹配 `123`。新增 `departmentIds` 分支已更准确，但兼容分支仍需测试。
- Liquibase：
  - `V13` 中 `CREATE INDEX idx_base_org_parent_id...; CREATE INDEX idx_base_org_group_id...; CREATE INDEX idx_base_user_org_user_org...;` 放在同一 changeset 且只有一个 precondition 检查 `idx_base_org_parent_id`。如果其中一个索引已存在、另一个不存在，MySQL 可能失败。
  - `V14` 只含 MySQL 兼容补丁，没有 H2 分支；若 H2 测试环境会执行 `db.changelog-master.yaml`，需确认不会因 dbms 过滤产生缺列。

## 3. Cursor 执行前准备

请在 Cursor 终端执行：

```powershell
cd D:\workspaces\JBM7
git status --short
```

确认未提交改动与上述范围一致，不要回退用户改动。

如果端口被占用：

```powershell
netstat -ano | findstr ":5555 :7777 :8888 :5173"
```

只停止确认是本项目启动的进程。

## 4. 构建验证

### 4.1 前端构建

PowerShell 直接执行 `npm` 可能被执行策略拦截，使用 `npm.cmd`：

```powershell
cd D:\workspaces\JBM7\jbm-admin-vue
npm.cmd ci --cache .\.npm-cache
npm.cmd run build
```

验收：

- `vue-tsc -b` 通过。
- `vite build` 通过。
- 检查构建产物中 `OrgTreeSelect`、`UserList`、`AppList` chunk 正常生成。

### 4.2 后端编译

优先做目标服务编译：

```powershell
cd D:\workspaces\JBM7
mvn -Pjaja7 -pl jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-auth,jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center,jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-gateway -am -DskipTests package
```

如果首次下载依赖超时，继续重跑一次；重点看最后的编译错误，不要被下载日志干扰。

验收：

- Auth、Center、Gateway 三个模块编译成功。
- 若失败，优先定位新增类：`BaseUserOrgService`、`OrgDataScopeHelper`、`BaseUserForm`、`BaseApp`。

## 5. 后端启动

推荐由 Cursor 使用 `.vscode/launch.json` 的复合启动项：

- `jaja7: Auth + Center + Gateway`

对应端口：

- Auth: `5555`
- Center: `8888`
- Gateway: `7777`

启动后验证：

```powershell
curl.exe http://127.0.0.1:5555/actuator/health
curl.exe http://127.0.0.1:8888/actuator/health
curl.exe http://127.0.0.1:7777/actuator/health
```

## 6. 后端 API 实测用例

### 6.1 登录链路

通过 Gateway 获取 token：

```powershell
curl.exe -X POST http://127.0.0.1:7777/oauth2/token `
  -H "tenantId: 0" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "grant_type=password&client_id=jbmSeedDevAppKey00000001&client_secret=jbmSeedDevSecret0000000001&username=admin&password=Admin@123&scope=all&loginType=PASSWORD&vcode=9999"
```

验收：

- 返回 `access_token`。
- `/current/user` 返回 `companyId/deptId` 字段。
- 如果 token 中可见 `appOrgId`，确认其来自 `BaseApp.orgId`。

### 6.2 组织树与默认组织

```powershell
curl.exe -X POST http://127.0.0.1:7777/baseOrg/tree -H "tenantId: 0" -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" -d "{}"
```

验收：

- 返回列表。
- 存在默认组织 `id=1` 或组织名“默认组织”。
- `orgCode/orgType/managerId/groupId` 等字段不会导致序列化异常。

### 6.3 应用所属组织

步骤：

1. 新增一个组织。
2. 新增一个应用，传入 `orgId=<新组织ID>`。
3. 查询 `/app?pageForm.currPage=1&pageForm.pageSize=10`。

验收：

- 新增应用成功。
- 列表返回对应 `orgId`。
- 不传 `orgId` 时确认后端行为：拒绝、默认值、或允许为空，需要与前端“必填”保持一致。
- 使用该应用 `client_id/client_secret` 登录时，`LoginPostProcessor` 能写入 `appOrgId`，但数据范围仍以登录用户 `companyId` 为准。

### 6.4 用户主组织、部门、跨组织授权

步骤：

1. 新建组织 A、组织 B，必要时在 A 下建部门 A1。
2. 新建用户 U，传 `companyId=A`、`departmentId=A1`、`orgIds=[B]`。
3. 调用 `GET /user/{userId}/orgs`。
4. 调用 `PUT /user/{userId}` 修改 `orgIds`。
5. 调用 `PUT /user/{userId}/orgs` 单独修改授权。

验收：

- 新建用户后 `base_user.company_id`、`department_id` 正确。
- `base_user_org` 有 B 的授权记录，不包含主组织 A。
- 更新为空数组时授权被清空。
- 给 admin/root 用户分配跨组织授权时，返回明确错误或被拒绝。
- 非数字 `orgIds` 返回可理解错误，不应 500 堆栈泄漏。

### 6.5 数据范围验证

构造数据：

- U1：公司 A，部门 A1。
- U2：公司 A，部门 A2。
- U3：公司 B。
- 登录用户 L：公司 A，部门 A1。

场景：

- L 无跨组织授权：`GET /user` 只应看到自己部门/下级范围内用户。
- L 授权 B：`GET /user` 应看到公司 B 用户；同时确认是否还能看到公司 A 全量用户。这里是关键权限验收点。
- Admin：应看到全部用户。

验收要点：

- 与产品预期一致。
- 如果“有跨组织授权后主组织范围扩大到整个公司”不是预期，需调整 `OrgDataScopeHelper#applyUserQueryScope`。

## 7. 前端页面实测

启动前端：

```powershell
cd D:\workspaces\JBM7\jbm-admin-vue
npm.cmd run dev -- --host 127.0.0.1 --port 5173
```

打开：

```text
http://127.0.0.1:5173
```

### 7.1 登录

- 使用 `admin / Admin@123` 登录。
- 验证 Gateway 代理正常，登录后进入管理页面。

### 7.2 用户管理

检查：

- 用户列表新增“所属组织”列，展示组织名称而不是乱码。
- 新建用户：
  - 主组织下拉可展开树形缩进。
  - 部门下拉可选。
  - 跨组织授权选项不包含主组织。
  - 保存后刷新列表仍显示主组织。
- 编辑用户：
  - 主组织/部门回显正确。
  - 跨组织授权复选框回显正确。
  - 修改授权保存后重新打开仍正确。

### 7.3 应用管理

检查：

- 应用列表新增“所属组织”列。
- 新建/编辑应用时不选组织应有前端校验。
- 选择组织保存后列表回显正确。
- 历史应用回填 `orgId=1` 后展示“默认组织”。

## 8. 自动化/脚本建议

仓库已有 REST 脚本：

- `scripts/run_auth_rest_tests.py`
- `scripts/run_center_rest_tests.py`
- `scripts/run_user_perm_rest_tests.py`
- `scripts/run_all_rest_tests.py`
- `scripts/run_org_plan_smoke.py`

当前 Windows 环境可能没有 `python`/`py` 命令。Cursor 执行前先确认 Python：

```powershell
python --version
py -3 --version
```

如果 Python 可用，优先跑：

```powershell
python scripts\run_all_rest_tests.py --profile jaja7 --wait 60 --base-url http://127.0.0.1:7777 --auth-url http://127.0.0.1:5555
```

注意：`scripts/run_org_plan_smoke.py` 当前文件显示有乱码/疑似语法损坏，请 Cursor 先打开检查编码和语法，再决定是否修复后执行。

## 9. 输出要求

Cursor 执行完成后，请在本计划下追加：

- 实际执行命令。
- 每项通过/失败结果。
- 失败接口的 HTTP 状态、响应摘要、后端日志文件位置。
- 需要修复的代码点，按严重程度排序。

建议追加标题：

```markdown
## 10. Cursor 执行结果
```

