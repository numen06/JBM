# 意见反馈

意见反馈属于 JBM Center 公共能力，建筑、物连及独立 JBM Web 的头像下拉菜单均提供入口。
所有已登录用户可以提交标题、类型、内容和可选联系方式。提交人、所属租户、应用由服务端认证信息记录，客户端不能覆盖。
提交成功仅返回反馈编号；普通用户、租户管理员均不能读取任何反馈列表、详情或处理结果。
标准角色 `platform_operator`（平台运营）以及超级管理员可以在反馈页面进入“反馈管理”，跨租户查看并处理当前部署中的反馈。
平台运营沿用已有标准角色的权限范围，不自动获得超级管理员专有的基础设施和保留角色管理权限。

## 部署归属

Center 配置 `jbm.feedback.deployment-mode` 支持 `local`（默认，本地/私有化）和 `cloud`（云平台）。
该参数由部署配置提供，不能由提交表单指定；切换参数影响新提交反馈的来源标记，保留历史标记。
两种模式均只写入当前 Center 所连接的数据库，没有自动上报外部云平台的流程。
各独立部署应使用自己的数据库；云部署运营方集中处理其本部署各租户的反馈。
现有 `FEIGE_GATEWAY_AUTHORIZATION_MODE` 用于网关授权期限，与本模块无关。

## 安装与验证

正常 Center 启动使用 Alembic 升级至 `20261104_29`，建立 `jbm_feedback`，并在历史安装缺失时补齐标准平台运营角色；不重置已有角色、账号和授权。
新安装仍沿用 `center/migrations/sql/local_seed.sql` 的标准角色种子数据。
接口：`GET /feedback/context`、`POST /feedback/submit` 面向登录用户；`GET /feedback`、`GET /feedback/{id}`、`PUT /feedback/{id}` 仅面向平台运营和超级管理员。
处理状态为待处理、处理中、已处理；处理备注为运营内部记录，更新携带 revision，避免多人处理相互覆盖。
运行 `python -m pytest center/tests/test_feedback.py` 可验证两种部署模式、身份防伪造、普通用户/租户管理员隔离及处理并发冲突。
