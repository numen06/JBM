# OpenAPI 文档中心实现验证摘要

日期：2026-06-03

## 实现摘要

在 Center 新增 OpenAPI Hub：存储/同步各服务 Swagger spec、索引 operation、关联 `base_api`、内部认证接口、发布快照与安全测试代理。管理端新增 `/api/docs`（JBM 原生 UI）；公开 `/docs` 仅读取 `/published-docs/**` 快照，不调用内部 `/api-docs/**`。

**说明：** 计划中的 `V20__openapi_docs.sql` 因已有 `V20__custom_forms_contract_patch.yaml` 占用，实际迁移为 `V21__openapi_docs.sql`。

**说明：** spec 解析首版使用 Fastjson 直接解析 Springfox `/v2/api-docs` JSON（paths/operations），未引入 `swagger-parser` 依赖（Maven  classpath 解析失败）；后续可再接入 swagger-parser 做 OpenAPI 3 校验。

## 主要修改文件

### 后端
- `jbm-cluster-api-basic`: `OpenApiDocument`, `OpenApiOperation`, `PublishedApiDoc`, forms/models
- `jbm-cluster-common-mysql`: V21 SQL, mappers, services (`OpenApiHubServiceImpl`, `PublishedApiDocServiceImpl`, …), `OpenApiSpecSanitizerTest`
- `jbm-cluster-platform-center`: `OpenApiDocsController`, `PublishedApiDocController`
- `BaseApiService`: `findApiByServicePathMethod`
- `AdminVueRbacSeedInitializer`: 菜单 `api_docs`、`api_registry` 分组
- `bootstrap.yml` (center): `/published-docs/**` permit-all
- `bootstrap.yml` (gateway): `/published-docs/**` permit-all

### 前端
- `src/api/openapiDocs.ts`, `src/api/types.ts`
- `src/views/api/ApiDocsPage.vue`
- `src/views/api/ApiRegistryList.vue`（query 筛选 + 查看文档）
- `src/views/docs/ApiWikiPage.vue`（已发布快照区）
- `src/constants/adminNav.ts`, `src/router/index.ts`

## 验证命令与结果

| 命令 | 结果 |
|------|------|
| `npm run build` (jbm-admin-vue) | **通过**（2026-06-03，vue-tsc + vite build，含 `ApiDocsPage`/`ApiWikiPage`/`openapiDocs` chunk） |
| `mvn -pl jbm-cluster/jbm-cluster-common/jbm-cluster-common-satoken install -DskipTests` + `mvn -pl jbm-cluster/jbm-cluster-common/jbm-cluster-common-mysql test -Dtest=OpenApiSpecSanitizerTest` | **通过**（2 tests：`filterPublishable_requiresOpenLinkedAndEnabled`、`buildPublishedSpec_containsPaths`） |
| `mvn -pl jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center -am compile -DskipTests` | **通过** |
| `mvnd` | **不可用**（`D:\workspaces\bin\mvnd.cmd` 未找到；改用 `mvn`） |

**Maven 说明：** 若本地未安装 `jbm-cluster-common-satoken`，直接 `-am test` 可能因 testCompile 依赖 `LoginHelper` 失败；先 `install` satoken 模块或完整 `-am` 编译依赖链即可。

## 安全配置（jaja7 / 公开文档）

| 配置项 | 位置 | 状态 |
|--------|------|------|
| Center `jbm.cluster.permit-all` | `bootstrap.yml` | 含 `/published-docs/**` |
| Center jaja7 profile | `bootstrap-jaja7.yml` | 未覆盖 permit-all，继承 `bootstrap.yml` |
| Gateway `jbm.api.permit-all` | `bootstrap.yml` | 含 `/**/published-docs/**` |
| Gateway jaja7 `auth-ignores` / `security.ignore.whites` | `bootstrap-jaja7.yml` | 含 `/published-docs/**` 与 `/**/published-docs/**` |

## API 端点

**内部（需登录 + 菜单 `api_docs`）**
- `GET /api-docs/sources`
- `GET /api-docs/spec/{serviceId}`
- `GET /api-docs/operations`
- `GET /api-docs/operations/{operationId}`
- `POST /api-docs/sync`
- `POST /api-docs/test`
- `POST /api-docs/export`
- `POST /api-docs/publish`

**公开**
- `GET /published-docs/openapi`
- `GET /published-docs/openapi/{docKey}`

## 遗留 / 后续可选

- 未做浏览器截图联调（需本地启动 auth/center/gateway）
- `GET /api-docs/export/{taskId}` 异步导出任务表未实现（首版同步导出）
- OpenAPI YAML / Postman / curl 包导出为可选增强
- swagger-parser 集成与 OpenAPI 3 `/v3/api-docs` 深度校验
- Scalar Reference 预览未引入（计划要求样式验收后再加）

## 运行中进程

验证完成后 **无** 遗留 Java/Spring/Maven/Node 进程。
