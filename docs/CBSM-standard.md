# CBSM 分层与事务规范

> **CBSM**：**C**ontroller → **B**usiness → **S**ervice → **M**apper
>
> 文档路径：`docs/CBSM-standard.md`（与 `CBSM分层与事务规范.md` 同内容，便于跨平台打开）
>
> 关联：`NamePrefixTransactionAttributeSource`、`MasterDataTransactionAutoConfiguration`、`BaseBusiness`

---

## 1. 分层职责

| 层级 | 包/命名 | 职责 | 禁止 |
|------|---------|------|------|
| Controller | `*.controller` | 入参、鉴权、调 Service/Business | 直连 Mapper、多表编排 |
| Business | `*.business` / `*BusinessImpl` | 跨实体编排、平台语义（登录/网关刷新等） | `extends ServiceImpl`、类级 `@Transactional` |
| Service | `*.service` / `*ServiceImpl` | 单实体 CRUD、领域规则 | 依赖 Business |
| Mapper | `*.mapper` | 数据访问 | 业务与事务边界 |

### 1.1 模块落点（cluster-center）

```
jbm-cluster-common-mysql/  → mapper / service / service.impl
jbm-cluster-platform-center/ → controller / business / business.impl
```

### 1.2 Controller 注入

| 场景 | 注入 |
|------|------|
| CRUD、分页查询 | `XxxService` |
| 编排、批量、网关刷新、登录注册 | `XxxBusiness` |

- **Business 不得 `extends XxxService`**
- **BusinessImpl**：`implements XxxBusiness` + `@Autowired XxxService`，独立 Bean
- **禁止** BusinessImpl `extends ServiceImpl`、**禁止** `@Primary`

---

## 2. 依赖规则

```
Controller → Business → Service → Mapper
              ↘ Service（可多个，避免环；必要时 @Lazy）
```

---

## 3. 事务控制

### 3.1 策略

- 默认：**不用类级 `@Transactional`**，用 **方法名前缀 AOP**
- 开关：`jbm.masterdata.transaction.enabled=true`（默认 true）
- 需：`spring-boot-starter-aop` + `PlatformTransactionManager`

```yaml
jbm:
  masterdata:
    transaction:
      enabled: true
      service-pointcut: execution(* com.jbm..service.impl..*(..))
      business-pointcut: execution(* com.jbm..business.impl..*(..))
```

实现：`MasterDataTransactionAutoConfiguration` + `NamePrefixTransactionAttributeSource`

### 3.2 解析顺序

1. 方法上 `@Transactional`（优先）
2. 方法名前缀匹配

### 3.3 写前缀 → REQUIRED（回滚 Exception）

`add*` `update*` `remove*` `clear*` `delete*` `save*` `insert*` `register*` `grant*` `import*` `bind*`
`activation*` `activate*` `close*` `reset*` `rest*` `sync*` `enable*` `disable*`
`patch*` `merge*` `copy*` `move*` `revoke*` `assign*` `login*` `publish*`

### 3.4 读前缀 → SUPPORTS, readOnly

`find*` `get*` `select*` `query*` `list*` `page*` `count*` `search*` `retrieval*`
`is*` `has*` `load*` `fetch*` `exists*` `check*` `build*`

新增前缀：改 `NamePrefixTransactionAttributeSource` 并更新本文档。

### 3.5 显式事务

| 场景 | 做法 |
|------|------|
| 传播/隔离与前缀不一致 | 方法级 `@Transactional` |
| Business 读名方法内要同事务块 | `BaseBusiness.executeInTransaction(...)` |
| 类级 `@Transactional` | **禁止** |

### 3.6 失效场景

| 反模式 | 结果 |
|--------|------|
| `this.saveEntity()` | 无代理，无事务 |
| `this.login()` 调另一写入口 | 内层无事务 |
| `@EventListener` 内 `this.save*` | 无事务边界 |
| `get*` 内 `this.save*` | 读前缀 + 自调用双失效 |

---

## 4. this 自调用拆解（CBSM 核心）

### 4.1 Business

```
public 写入口（AOP）
  ├── private doXxx()      // 类内复用，共享外层事务
  └── otherService.xxx() // 跨 Bean，走对方代理
```

- 禁止：`this.saveEntity()`、`this.login()` 等调同类另一 **public 写方法**
- 示例：`BaseUserBusinessImpl` — `saveEntity` → `doAddUser`/`doUpdateUser`；`login*` → `doLogin`

### 4.2 Service

| 场景 | 做法 |
|------|------|
| 同入口多步 | `private persistXxx()`，由 `saveEntity`/`addXxx` 调用 |
| 事件/监听器/读路径写库 | `@Lazy` 注入自身接口 `self`，`self.saveEntity()` |
| 删除链需代理 | `self.removeMenu(id)` |
| 跨实体 | 注入 `YyyService` |
| 简单转发且无重写逻辑 | 可用 `super.saveEntity()`（外层已有事务） |

- 示例：`BaseMenuServiceImpl.persistMenu`、`BaseAuthorityServiceImpl` 事件用 `self.saveOrUpdateAuthority`

---

## 5. Code Review 清单

- [ ] BusinessImpl 未 extends ServiceImpl、无 @Primary
- [ ] Business 未 extends Service
- [ ] Controller：查 Service，编排 Business
- [ ] 无类级 @Transactional
- [ ] 无 this 调同类 public 写方法
- [ ] 监听器/ApplicationListener 写库用 @Lazy self 或独立 Service 写入口
- [ ] 新方法名匹配写/读前缀或显式 @Transactional

---

## 6. 代码索引

| 项 | 路径 |
|----|------|
| 事务前缀 | `jbm-framework-data-masterdata/.../NamePrefixTransactionAttributeSource.java` |
| 自动配置 | `jbm-framework-autoconfigure-mybatis/.../MasterDataTransactionAutoConfiguration.java` |
| Business 基类 | `.../business/BaseBusiness.java` |
| 文档锚点 | `.../transaction/CbsmTransactionRules.java` |
| Center 包说明 | `jbm-cluster-platform-center/.../business/package-info.java` |

---

## 7. 修订

| 日期 | 说明 |
|------|------|
| 2026-05-21 | 首版：CBSM 四层、方法名事务、this 拆解 |
