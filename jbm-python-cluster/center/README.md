# JBM Python Center

Python Center 采用 IoT Platform 的“业务模块优先、模块内分层”规范。

Python Center 独立承担全部 Center 接口，不依赖 Java Center，不包含 Java 回退或运行时转发。

已实现 Java Center 基线的 187 个公开路由及 4 个内部契约，覆盖：

- `/current/user`、`/current/user/menus`、`/current/dashboard`
- `/user` 查询及用户的角色、组织、账号查询
- `/baseOrg/root|tree|pageList`
- `/baseDic/root|list|root/pageList|items/pageList|getDicMap`
- `/app`、`/role`、`/role/all`、`/gateway/routes` 及完整 CRUD
- Authority、API Key、网关限流/IP 策略与授权关系
- 开发者、扩展字段、自定义表单、数据源
- OpenAPI 集群发现、同步、测试、导出和发布

数据库结构由 `center/migrations` 中的 Alembic 基线管理；本地技术平台种子数据由
`jbm_cluster_py.platform.center.bootstrap.seed` 幂等初始化。Docker 启动顺序固定为：

```text
alembic upgrade head -> Python seed -> Python Center
```

路由覆盖数量由契约测试锁定；任何未实现路径都视为发布失败。
