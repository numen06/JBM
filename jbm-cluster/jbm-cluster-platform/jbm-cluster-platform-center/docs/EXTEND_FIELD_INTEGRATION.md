# Center 扩展字段接入说明

> 通用使用方案见仓库根目录 [docs/动态字段使用方案.md](../../../../docs/动态字段使用方案.md)。

## 架构

- **配置真源**：MySQL `extend_form_definition`（按 `tenant_id` + `form_code` 唯一）。
- **运行时缓存**：Redis `extend_field:form:{tenantId}:{formCode}`。
- **Center 双重角色**：
  - 配置发布：`/extend-field/forms`、`/customForms/saveData`（`code` = `formCode`）。
  - 运行时消费：`@EnableExtendField(source = REDIS)`，业务 API 传 `formCode` + 平铺字段，写入 `extend_data`。

## Center 配置（Nacos / bootstrap）

```yaml
jbm:
  extend-field:
    enabled: true
    source: REDIS
    auto-flatten: true
    sync-local-to-redis-on-startup: false
    builtin-definition-controller-enabled: false
    tenant:
      enabled: true
      header: tenantId
      default-tenant-id: "0"
      use-default-when-missing: true
```

## 管理 API（Center）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/extend-field/forms/{formCode}` | 保存并发布 |
| PUT | `/extend-field/forms/{formCode}` | 更新并发布 |
| POST | `/extend-field/forms/{formCode}/publish` | 从库刷新 Redis |
| GET | `/extend-field/forms/{formCode}` | 读库 |
| GET | `/extend-field/forms/{formCode}/definitions` | 读 Redis 当前定义 |

租户：请求 Header/参数 `tenantId` 优先，否则登录用户 `appId`，否则默认模块 `0`。

## CustomForms 联动

- 表单 `code` 即 `formCode`。
- `custom_forms.detail` 保存前端设计器完整 JSON（`formItems` + `formConfig`）；`extend_data` 不存表单设计。
- `custom_forms_item` 保存可平铺字段明细，发布时只转换运行时字段子集。
- `saveData` 成功后默认 `autoPublishExtendField=true`，将 `CustomFormsItem` 映射为 `FieldDefinition` 并发布。

## 其它微服务接入

**依赖：**

```xml
<dependency>
  <groupId>com.jbm</groupId>
  <artifactId>jbm-framework-autoconfigure-extendfield</artifactId>
</dependency>
<dependency>
  <groupId>com.jbm</groupId>
  <artifactId>jbm-framework-autoconfigure-redis</artifactId>
</dependency>
```

**启动类：**

```java
@EnableExtendField(source = FieldDefinitionSource.REDIS)
```

**配置：** 与 Center 相同的 `jbm.extend-field`（`source: REDIS`，租户 Header 与网关一致）。

**配置变更：** Feign 调用 `ExtendFormDefinitionClient` → Center `POST /extend-field/forms/{formCode}`。

**业务请求：** JSON 中带 `formCode` 与扩展字段平铺键；实体继承 `MasterDataEntity` 且表含 `extend_data` JSON 列。

## Feign 客户端

`com.jbm.cluster.api.service.feign.client.ExtendFormDefinitionClient`（服务名 `jbm-cluster-platform-center`）。

## 示例

双服务本地示例见仓库 `jbm-examples/EXTEND_FIELD_DUAL_SERVICES.md`。
