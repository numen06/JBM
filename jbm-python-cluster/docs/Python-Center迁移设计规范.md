# Python Center 迁移设计规范

本规范提炼自 `iot-platform/docs/python-engineering-architecture-standard.md` 与
`iot-platform/docs/http-api-design-standard.md`，用于 JBM Center 的纯 Python 完整实现。

## 1. 强制边界

- Center 是独立可部署应用，禁止导入 auth、gateway、job 等应用代码；协作仅经 HTTP、消息或稳定公共契约。
- 按业务模块组织代码，模块内部依赖方向固定为 `API -> Application -> Domain`，Infrastructure 实现 Domain Port，Bootstrap 负责组装。
- `main.py` 只暴露应用和启动入口；Router 不直接操作 SQLAlchemy、事务或外部客户端。
- `common` 只放通用机制，不放 Center 的用户、组织、权限等业务决策。
- import 阶段不得连接数据库、Nacos 或启动线程。

## 2. 接口契约

- 现有 JBM 管理端路径属于兼容边界，迁移期间保持 `{success, code, message, result}` 与 Java 字段名。
- 新增能力使用 `/v1/...`，遵循正确 HTTP 状态、RFC 9457 错误、`data` 单一载荷和服务端分页。
- 超过 JavaScript 安全范围的 ID 输出字符串；密码、Token、密钥不得出现在响应或日志。
- 除健康检查、OpenAPI 和明确内部契约外，接口必须通过 Auth userinfo 校验访问令牌。

## 3. 数据所有权

- Python Alembic 是 Center Schema 的唯一所有者，应用进程启动时不执行 DDL。
- Alembic 先兼容标记已有数据库，再为空库创建完整 Center Schema；应用启动前由部署命令显式执行迁移。
- Repository 是唯一 SQLAlchemy 入口；Application 定义事务边界，API 不得 `commit()`。

## 4. 迁移顺序

1. 以 Java Center Swagger 作为一次性兼容基线，所有路径由 Python 原生实现。
2. 写接口按聚合边界处理，权限和凭证写入必须保留验证、授权和密文存储。
3. 契约测试锁定 187 个公开路由及 4 个内部路由，禁止运行时转发 Java。
4. 空库验收必须经过 `Alembic -> seed -> OAuth2 PKCE -> Center API` 完整链路。
5. Compose 中 Center 只能使用 Python 镜像，Java Center 不再打包、启动或注册。

## 5. 发布门禁

```powershell
python -m ruff check center common integrations
python -m pytest -q center/tests
```

必须额外验证：OAuth 登录、当前用户和菜单、基础治理页面、权限隔离、空库种子数据、Nacos 注册、
数据库连接回收，以及路由清单 100% 覆盖。
