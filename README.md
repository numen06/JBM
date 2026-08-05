# JBM 7.3 Python

JBM 7.3 的纯 Python 基础技术平台分支，不包含 Java 服务或 Maven 构建链。

## 组成

- `jbm-admin-vue`：Vue 3 管理后台与可发布 npm 包
- `jbm-python-cluster`：FastAPI 集群服务
  - Center `7777`
  - Auth `5555`
  - Gateway `6060`
  - Doc `9999`
  - Push `3313`
  - Job `4444`
- MySQL、Redis、RabbitMQ、MinIO
- r-nacos `0.6.6`

## 本地启动

```powershell
.\scripts\start_local_docker.ps1
```

前端地址：`http://127.0.0.1:5173`。本地初始化三种身份：

- 超级管理员：`admin / Admin@123`
- 平台运营：`platform_operator / Operator@123`，可跨租户治理平台资源
- 示例租户管理员：`tenant_admin / Tenant@123`，固定绑定租户 `2000`

生产环境必须通过 `JBM_CONFIG_JSON` 或配置中心覆盖所有初始密码，并在首次交付后停用种子密码。

## 多租户边界

- 平台域：`super_admin`、`platform_operator`，负责租户、权限、网关、平台审计与技术资源治理。
- 租户域：普通租户角色必须绑定 `tenantId`；Center 会强制按租户过滤用户、组织和应用，忽略客户端伪造的跨租户筛选条件。
- 前端菜单只负责体验，后端 403 才是安全边界。租户直接访问平台路由或其他租户用户时会被拒绝。
- 当前完成的是基础技术平台的租户主数据边界；后续业务模块接入时，业务表必须持久化 `tenant_id` 并在仓储层统一注入过滤条件。

启动时 Center 会先执行 Alembic，再执行幂等种子初始化。验证脚本通过 OAuth2 + PKCE 登录，并检查用户、组织、字典、应用、角色和网关路由。

## 单独验证

```powershell
cd jbm-admin-vue
npm run build
npm test

cd ..\jbm-python-cluster
python -m pytest -q

cd ..
.\scripts\verify_local_seed.ps1
```
