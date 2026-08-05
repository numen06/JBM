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

默认管理员：`admin / Admin@123`，前端地址：`http://127.0.0.1:5173`。

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
