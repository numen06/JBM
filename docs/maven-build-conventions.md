# Maven 构建与测试规范

> JBM7 默认在父 POM 中关闭单元测试，以缩短日常 `compile` / `package` 耗时。需要跑测时**仅在命令行**显式开启，**禁止**在子模块 `pom.xml` 里写 `<skipTests>false</skipTests>` 或 `<skipTests>true</skipTests>` 覆盖父级。

---

## 一、默认行为（父 POM）

| 父 POM | surefire 默认 | 说明 |
|--------|---------------|------|
| 根 `pom.xml` | `skip=true`、`skipTests=true` | 聚合 `jbm-framework`、`jbm-util`、`jbm-examples`、`jbm-cluster` 时生效 |
| `jbm-framework/pom.xml` | 同上 | 框架子模块 |
| `jbm-cluster/pom.xml` | `skipTests=true` | 集群子模块 |

日常构建（默认跳过测试）：

```bash
mvn clean package -pl jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center -am
```

---

## 二、子模块 POM 约定

1. **不要**在子模块 `pom.xml` 中配置 `<skipTests>false</skipTests>` 强制开启测试。
2. **不要**在子模块中重复写 `<skipTests>true</skipTests>`（与父级重复，无意义）。
3. 若仅需调整 surefire 行为（如 fork 超时），可保留 `maven-surefire-plugin` 的**非 skip** 配置，例如 `forkedProcessTimeoutInSeconds`。
4. 模块级 `maven-deploy-plugin` 的 `<skip>true</skip>`（跳过发布）与测试无关，可保留。

---

## 三、命令行执行测试

从仓库根目录执行时，需同时覆盖父级的 `skip` 与 `skipTests`（根 / `jbm-framework` 两层均配置了 `skip=true`）：

```bash
mvn test -DskipTests=false -Dskip=false -pl <模块路径> -am
```

仅跑指定测试类：

```bash
mvn test -DskipTests=false -Dskip=false \
  -pl jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center -am \
  -Dtest=CenterRbacH2IT
```

仅编译、仍跳过测试（与日常打包一致）：

```bash
mvn clean compile -DskipTests
```

---

## 四、常用模块示例

### Center H2 集成测试

```bash
mvn test -DskipTests=false -Dskip=false \
  -pl jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center -am \
  -Dtest=CenterRbacH2IT
```

### common-mysql H2 测试

```bash
mvn test -DskipTests=false -Dskip=false \
  -pl jbm-cluster/jbm-cluster-common/jbm-cluster-common-mysql -am \
  -Dtest=OAuthClientSecretVerifierH2IT
```

### 字典模块单测

```bash
mvn test -DskipTests=false -Dskip=false \
  -pl jbm-framework/jbm-framework-autoconfigure/jbm-framework-autoconfigure-dictionary -am
```

### platform-job（含 Spring 上下文，建议带超时）

父级已配置 `forkedProcessTimeoutInSeconds=300`，开启测试时：

```bash
mvn test -DskipTests=false -Dskip=false \
  -pl jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-job -am
```

### jbm-examples（示例模块）

示例聚合 POM 默认 `maven.test.skip=true`；构建可运行示例时使用 profile：

```bash
mvn package -Pbuild-examples -pl jbm-examples/jbm-examples-mysql -am
```

---

## 五、本地启动（非 Maven 测试）

Spring Boot 本地运行与 surefire 无关，例如 Center H2 profile：

```bash
mvn -pl jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center -am spring-boot:run \
  -Dspring-boot.run.profiles=h2
```

更多 H2 说明见：`jbm-cluster-platform-center/src/test/resources/README-h2.md`。

---

## 六、CI 建议

- 默认流水线：`mvn package`（不追加 `-DskipTests=false`），保持快速反馈。
- 独立测试 Job / 夜间构建：按上文第三节追加 `-DskipTests=false -Dskip=false`，并按模块 `-pl` 拆分，避免一次跑全仓单测。
