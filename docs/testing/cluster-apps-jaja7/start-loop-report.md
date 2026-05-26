# 集群应用启动循环报告（2026-05-26）

## 范围

- **目标**：auth、center、gateway、doc、push、logs、job、bigscreen
- **排除**：weixin（本轮不作为退出条件）

## 循环摘要

| 轮次 | 构建 | 启动 | 主要问题 | 动作 |
|---:|---|---|---|---|
| 1 | 通过 | 8 服务并行 `--prepare compile` 全 FAIL | mvnd 仓库锁竞争；push 误删源码导致 ClassNotFound | 修正 EmailPushConfigMapper namespace |
| 2 | 通过 | `--prepare none` 仍 FAIL | push 大量 D 状态文件；exec:java Redis/LatencyUtils | `git restore` 恢复 push 模块；Redis 启动降级补丁 |
| 3 | 通过 | 核心 trio + 单 auth 仍 FAIL | spring-boot:run exit 1；Redis/依赖 | 调整 `jbm_cluster_ops.py` 为 `-f pom.xml spring-boot:run` |

## 构建

```text
mvnd -pl jbm-cluster-platform-auth,center,gateway,doc,push,logs,job,bigscreen -am -DskipTests compile
→ BUILD SUCCESS（center 曾需单独 clean compile）
```

## 启动（未达退出条件）

推荐命令（计划约定）：

```powershell
python scripts\jbm_cluster_ops.py start auth center gateway doc push logs job bigscreen --background --clean --prepare compile
python scripts\jbm_cluster_ops.py wait auth center gateway doc push logs job bigscreen --timeout 240
```

本轮实测：并行启动 + 240s wait 全部 `[FAIL]`。日志要点见 `logs/ops-start-*.log`：

- **auth**：`RedisConnectionFailure`（10.100.10.62:6379）、`LoginAssemblyConfiguration` / `BaseAppPreprocessing` 初始化（已加 try/catch 降级）
- **center/gateway/doc 等**：并行 `prepare compile` 时 `Could not open file channel ... .locks`
- **push**：误删 `PushMessageItemServiceImpl` 等（已 restore）；`EmailPushConfigMapper` namespace 已改为 `com.jbm.cluster.push.mapper.EmailPushConfigMapper`

## 冒烟

```powershell
python scripts\run_cluster_apps_smoke_tests.py --profile jaja7 --base-url http://127.0.0.1:7777 --auth-url http://127.0.0.1:5555 --services doc,push,logs,job,bigscreen
```

- 结果：`.cursor/cluster-apps-smoke-result.json`
- 核心未监听 → login 失败 → 非核心用例 **skipped**

## 代码/脚本变更（增量）

1. `EmailPushConfigMapper.xml` namespace 与 Java Mapper 对齐
2. 从 `HEAD` 恢复 push 模块误删的 business/controller/mapper/service/xml
3. `BaseAppPreprocessing`、`LoginAssemblyConfiguration`：Redis 不可用时不阻断启动
4. `scripts/jbm_cluster_ops.py`：`prepare` 后统一从仓库根目录执行；运行改为 `mvnd -f <module>/pom.xml -am spring-boot:run`（避免 exec 类路径与错误 reactor 模块）

## 下一步建议

1. 先 **顺序** 启动：`auth` → wait → `center` → `gateway`，`--prepare none`（先全量 compile 一次）
2. 确认 Nacos `jbm7` + Redis `10.100.10.62:6379` 可用；或补 `application-jaja7.yml` 本地 Redis/DB 覆盖
3. 核心就绪后再启动 doc/push/logs/job/bigscreen 并跑冒烟

验证结束后已执行 `ops stop` 释放端口。
