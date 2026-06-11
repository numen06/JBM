# OpenAPI 文档可读化与在线测试优化方案

## 目标
把现有 OpenAPI 文档中心从“能同步、能导出原始清单”升级为“可在线预览、可下载、可页面调试”的接口文档能力。

最终效果：
- 管理端可以从 Swagger `/v2/api-docs` 同步接口。
- 页面能以 Apifox 类似的结构展示接口文档，而不是只展示 raw JSON。
- Markdown / HTML 下载包含请求参数、请求体、响应结构、示例、错误信息和治理信息。
- 页面安全测试可以正常发送请求，并把失败原因明确展示出来。

## 背景
- 当前服务已经有 Springfox Swagger 2，服务原始 spec 地址为 `/v2/api-docs`。
- 网关默认地址为 `http://127.0.0.1:6060`，文档同步会尝试 `/{alias}/v2/api-docs`。
- 现有管理端已经有 `API 文档与调试` 页面和 `/api-docs/export`、`/api-docs/test` 接口。
- 当前导出质量差的直接原因是后端 `writeMarkdown` / `writeHtml` 只输出接口标题、摘要和简单治理字段，没有解析 schema。
- 当前页面测试失败的高风险点包括：前端不生成 path 参数和示例 body，后端 query/path 未 URL 编码，代理异常信息不完整，RestTemplate 未配置超时，响应错误体没有稳定回传。

## 当前证据
- Swagger 原始同步入口：`OpenApiHubServiceImpl.fetchSpec` 会尝试 `/{alias}/v2/api-docs`、`/{alias}/v3/api-docs`、`/{serviceId}/v2/api-docs`。
- 服务别名规则：`OpenApiRouteAliasSupport` 把平台服务映射为 `center`、`auth`、`job`、`push`、`logs` 等短路径。
- 页面测试入口：`ApiDocsPage.vue` 调用 `testOpenApiOperation`，只提供 Query JSON、Headers JSON、Body 文本框。
- 后端测试代理：`OpenApiTestProxyService.execute` 负责拼接网关 URL、透传 Bearer Authorization、调用目标接口。
- 当前导出入口：`OpenApiHubServiceImpl.export` 支持 JSON / MARKDOWN / HTML / YAML，但 Markdown / HTML 内容极薄。

## 子 Agent 分工

### 子任务 1：测试失败链路定位
- 执行者：建议使用子 agent
- 输入：
  - `jbm-admin-vue/src/views/api/ApiDocsPage.vue`
  - `jbm-admin-vue/src/api/openapiDocs.ts`
  - `jbm-admin-vue/src/api/request.ts`
  - `jbm-cluster/jbm-cluster-common/jbm-cluster-common-mysql/src/main/java/com/jbm/cluster/common/mysql/service/openapi/OpenApiTestProxyService.java`
  - `jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center/src/main/java/com/jbm/cluster/center/controller/OpenApiDocsController.java`
- 任务：
  1. 确认前端实际请求路径是否为 `/center/api-docs/test`。
  2. 确认 Authorization、tenantId 是否传入 `/api-docs/test`。
  3. 使用一个免认证 GET 接口和一个需认证 GET 接口分别测试。
  4. 记录失败类型：路由 404、401/403、参数缺失、body 格式错误、代理异常、服务不可达。
- 输出：
  - 一份失败原因清单，按可修复优先级排序。
- 验证：
  - 至少拿到一个成功的 GET 测试结果。
  - 至少拿到一个失败测试的结构化错误返回。

### 子任务 2：后端测试代理增强
- 执行者：主 agent
- 输入：
  - `OpenApiTestProxyService.java`
  - `OpenApiTestRequest.java`
  - `OpenApiTestResult.java`
- 任务：
  1. 使用带超时配置的 `RestTemplate` 或 `RestTemplateBuilder`，避免页面长时间卡住。
  2. 对 path 参数和 query 参数做 URL 编码。
  3. 捕获 `HttpStatusCodeException`，把目标接口返回的 status、headers、bodyPreview 写入 `OpenApiTestResult`，而不是只显示异常 message。
  4. 捕获连接失败、超时、DNS/路由失败时，返回明确 `errorCode` 或 `errorMessage`。
  5. 支持从页面显式传入 Authorization，或继续复用当前登录 Authorization，但要在返回结果里标明实际是否带了认证头。
  6. 为 Content-Type 做默认值：有 body 且未指定时默认 `application/json;charset=UTF-8`。
- 输出：
  - 后端补丁。
  - 必要时扩展 `OpenApiTestResult` 字段，例如 `requestUrl`、`requestHeaders`、`responseHeaders`、`errorType`。
- 验证：
  - `mvn -pl jbm-cluster/jbm-cluster-common/jbm-cluster-common-mysql -am test` 或至少相关模块编译通过。
  - 手动 curl `/center/api-docs/test` 能返回结构化测试结果。

### 子任务 3：前端测试表单改造
- 执行者：主 agent
- 输入：
  - `ApiDocsPage.vue`
  - `types.ts`
  - `operationDetail.parametersJson`
  - `operationDetail.requestBodyJson`
  - `operationDetail.examplesJson`
- 任务：
  1. 从 `parametersJson` 自动拆出 path、query、header 参数，生成表格输入，不再要求用户手写整段 JSON。
  2. 从 `requestBodyJson` / schema 生成默认 body 示例。
  3. path 参数未填写时禁止发送，并提示具体参数名。
  4. header 参数允许用户添加，但默认隐藏敏感头；Authorization 默认使用当前登录 token。
  5. 返回结果拆成状态码、耗时、响应头、响应体、错误说明，不只显示一段 JSON。
  6. 对写操作保留二次确认。
- 输出：
  - 页面补丁。
- 验证：
  - `npm run build` 通过。
  - 浏览器页面可完成一次 GET 测试、一次 POST 测试或明确展示 POST 失败原因。

### 子任务 4：可读文档模型抽象
- 执行者：建议使用子 agent
- 输入：
  - `OpenApiHubServiceImpl.java`
  - `OpenApiOperation` 实体
  - 已保存的 `parametersJson`、`requestBodyJson`、`responsesJson`、`schemasJson`、`rawOperationJson`
- 任务：
  1. 设计内部文档视图模型，例如 `OpenApiReadableDoc`、`ReadableOperation`、`ReadableParameter`、`ReadableSchemaField`。
  2. 支持 Swagger 2 的 `$ref`、`schema`、`parameters in body`、`responses`。
  3. 兼容少量 OpenAPI 3 字段，例如 `requestBody`。
  4. 保留治理字段：服务、是否开放、是否认证、API 资源绑定状态。
- 输出：
  - 文档视图模型设计和解析工具类。
- 验证：
  - 对 `center`、`auth` 至少各抽样 3 个接口，能生成请求参数和响应结构。

### 子任务 5：Markdown / HTML 导出升级
- 执行者：主 agent
- 输入：
  - 子任务 4 的可读文档模型
  - `OpenApiExportRequest`
  - `OpenApiHubServiceImpl.export`
- 任务：
  1. Markdown 输出目录、服务分组、标签分组。
  2. 单接口输出：接口说明、方法路径、认证要求、请求头、路径参数、查询参数、请求体、响应参数、请求示例、响应示例、错误说明。
  3. HTML 输出完整可读页面，包含侧边目录、搜索、接口折叠、代码块。
  4. JSON / YAML 导出继续输出可导入工具的标准 spec，不要被可读文档模型破坏。
  5. 文件名按服务和时间生成，避免所有导出都叫 `openapi.md`。
- 输出：
  - 后端导出补丁。
- 验证：
  - 导出的 Markdown 能直接阅读。
  - 导出的 HTML 双击打开可浏览。
  - JSON 能继续被 Apifox/Postman 导入。

### 子任务 6：在线预览
- 执行者：主 agent
- 输入：
  - `ApiDocsPage.vue`
  - `ApiWikiPage.vue`
  - 导出 HTML 或新增 preview API
- 任务：
  1. 管理端增加“在线预览”按钮。
  2. 在线预览使用可读文档视图，不显示 raw spec。
  3. 公开 Wiki 的“已发布开放 API 文档”从 `<pre>` raw JSON 改为可读文档，至少先支持 HTML 片段或结构化渲染。
  4. 保留 raw spec 下载入口，供工具导入。
- 输出：
  - 前端预览补丁。
  - 如有必要，新增 `/api-docs/preview` 或扩展 `/api-docs/export` 支持 inline。
- 验证：
  - 管理端能预览当前筛选或勾选接口。
  - 公开 Wiki 能预览已发布文档。

## 步骤
1. 先修测试代理，因为页面调试失败会影响后续验收。
2. 把前端测试表单从 JSON 文本框升级为参数化表单，同时保留高级 JSON 模式。
3. 抽象可读文档模型，避免 Markdown、HTML、页面预览各写一套解析逻辑。
4. 重写 Markdown / HTML 导出，优先保证内容完整和可阅读。
5. 增加在线预览入口，管理端和公开 Wiki 分开处理。
6. 最后补测试和验证脚本，覆盖同步、导出、测试三条链路。

## 验证
- 后端：
  - `mvn -pl jbm-cluster/jbm-cluster-common/jbm-cluster-common-mysql -am test`
  - `mvn -pl jbm-cluster/jbm-cluster-platform/jbm-cluster-platform-center -am test`
  - 至少手动验证：
    - `GET http://127.0.0.1:6060/center/v2/api-docs`
    - `POST http://127.0.0.1:6060/center/api-docs/test`
    - `POST http://127.0.0.1:6060/center/api-docs/export`
- 前端：
  - `cd jbm-admin-vue && npm run build`
  - 浏览器验证：
    - 同步接口。
    - 打开接口详情。
    - 发送 GET 测试。
    - 发送带 body 的 POST 测试或看到明确失败原因。
    - 下载 Markdown。
    - 下载 HTML。
    - 在线预览可读文档。

## 完成标准
- 页面测试不再只显示“测试失败”，而是展示目标 URL、状态码、耗时、响应体或明确错误类型。
- 典型 GET 接口可从页面测试成功。
- 缺 path 参数、缺认证、服务不可达、业务 4xx/5xx 都有明确提示。
- Markdown / HTML 文档能让没有代码背景的人读懂接口怎么调用。
- JSON spec 下载仍能用于 Apifox/Postman 导入。

## 风险与处理
- 风险：Swagger 2 schema 中 `$ref` 解析不完整。
  - 处理：第一阶段支持常见 DTO、数组、Map、基本类型；复杂泛型标记为“结构待补充”并保留 raw schema。
- 风险：部分接口没有完整注解，生成的参数说明为空。
  - 处理：类型和必填项从 schema 推断，说明为空时显示字段名，不伪造业务含义。
- 风险：测试代理可被滥用访问内部地址。
  - 处理：只允许通过已同步的 operationId 测试，目标只走配置的 gatewayBaseUrl + service alias，不允许用户传任意 URL。
- 风险：写操作测试造成数据变更。
  - 处理：保留二次确认，页面明确标记写操作；后续可增加只允许测试开放接口或测试环境开关。

## 建议优先级
1. P0：后端测试代理结构化错误和 URL 编码。
2. P0：前端测试表单可正常填 path/query/header/body。
3. P1：Markdown / HTML 可读导出。
4. P1：管理端在线预览。
5. P2：公开 Wiki 可读化和发布快照优化。
