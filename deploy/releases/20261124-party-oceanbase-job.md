# 党校 OceanBase 备份兼容修复

2026-11-24 更新实际 DEBUG 和党校的 JBM Job。OceanBase 的事件目录为空时，Job 跳过会触发不兼容 `SHOW EVENTS` 的导出参数，保留表、视图、例程和触发器的原有导出流程。

## 版本与制品

| 核对项 | 结果 |
| --- | --- |
| 主线与版本 | `py-7.3` · `py-7.3-20261124.2` |
| 冻结提交 | `ee34c7bcf1e88be26dab7f7c907b048e70662c3f` |
| 修复提交 | `988f1a5fa4f3a41d0e5df3fe354e1e398450c0aa` |
| App2Docker | `jbm-py-7.3` · `94285f38-825c-45e7-ad9f-177f55ab79b0` |
| Dockerfile 和运行阶段 | `Dockerfile.app2docker` · `jbm-cluster-platform-job` |
| 镜像清单 | `sha256:63ea939249ebafa837ed4d0c5725ea6f7dfc4fbb42450c6e7982d9d640bce523` |
| AMD64 | `sha256:af1bd5f32f2ef41fd0da33452808f4754c25fb24ba64e09581a879a3c5820b93` |
| ARM64 | `sha256:d250003d3c4732eeaa09f7b09987bc663f85d4413f567bff5a4ae60d2b990e5e` |

## 回归

- DEBUG 在原域名 `https://debug5175.feige.51jbm.cn` 复测登录、建筑总览、设备目录及 JBM 11 项只读合约。实际 Job 容器 `162f09cb304e` 健康。
- 11:28 党校 Job 容器 `6932016bfd34` 健康并取得备份执行器的工作锁。原运行环境和三个挂载保留。
- 在 `https://jxdx.project.hz-aitech.com` 用 `admin` 密钥登录，JBM 11 项合约通过。打开数据库运维页面，备份源和任务历史实际加载成功。默认数据源为 `jxdx-oceanbase:2881/jbm_73`。建筑总览加载成功，项目设备目录返回 2,431 台。
- 实际备份和隔离还原演练已用相同修复代码通过。此次党校正式验收覆盖页面、备份源、任务历史和备份执行器启动。

## 回滚

只替换了 DEBUG 和党校的 Job。党校上一容器保留为 `jbm73-standard-test-jbm-cluster-platform-job-1-before-backup-fix`，旧镜像标识 `sha256:4abbcac54a50d8c56425a2d606509ff1d82155a6b536f541a031209655048757`。完整容器规格和运行配置保存在党校主服务器的迁移记录目录。回滚沿用原容器规格与挂载。
