# 日志留存设置

超级管理员可在日志管理的采集设置页分别保存登录日志、操作日志和访问日志的留存天数。页面的当前值来自 Loki 已加载的配置。保存后，日志服务将文件的 SHA-256 与 Loki 已解析并加载的运行配置摘要核对，一致后才返回当前生效天数。

接入已有环境时，在 Loki 原配置中启用以下运行配置。保留原存储、租户、全局周期和 Compactor 配置。

```yaml
runtime_config:
  file: /run/jbm-audit-retention/overrides.yml
  period: 2s
```

为该文件准备独立的持久目录。新目录中的 `overrides.yml` 初始化为 `overrides: {}` 并保留末尾换行，已有文件应原样保留。让日志服务的运行用户能在目录中原子替换文件，并让 Loki 运行用户能读取。将同一目录挂载到两个容器的 `/run/jbm-audit-retention`，Loki 使用只读挂载。

仅向日志服务设置 `JBM_AUDIT_LOKI_ADMIN_URL`，指向内部 Loki HTTP 地址。该连接需要读取 `/config` 和 `/metrics`。本地 Compose 可叠加使用 `compose.audit-retention.yml`，并设置 `JBM_AUDIT_LOKI_CONFIG` 和 `JBM_AUDIT_RETENTION_DIR`。保留公网和 Loki 读写代理原有的访问限制。

初次启用时三类日志沿用实际全局留存值。验证应先在隔离环境保存不同天数、回读并重启核对持久化。缩短周期可能清理超过新期限的记录，生产验收可保存与当前值相同的策略。
