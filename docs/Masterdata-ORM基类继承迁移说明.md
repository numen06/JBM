# Masterdata ORM 基类继承：老体系 -> 新体系迁移说明

> 适用版本：**7.3.x**
> 配套：[Masterdata-ORM-7.3体系.md](Masterdata-ORM-7.3体系.md)、[动态字段使用方案](动态字段使用方案.md)、[项目架构.md](项目架构.md)

---

## 1. 变更摘要

7.3 将 **多层实体基类** 收敛为 **单一 MasterDataEntity**，策略为 **基类放宽、子类收紧**（@TableField(exist = false)）。

| 维度 | 老体系 | 新体系 |
|------|--------|--------|
| 实体基类 | MasterDataIdEntity / MasterDataTreeEntity / MasterDataCodeEntity / MultiPlatform* | 仅 MasterDataEntity |
| 无表列 | 不继承对应基类 | 子类 exist = false |
| Service 泛型 | extends MasterDataTreeEntity 等 | extends MasterDataEntity |
| 已删除 | IMasterDataCodeService、IMultiPlatform* 及 Impl | IMasterDataService / IMasterDataTreeService |

业务 ORM 仍为 **MyBatis-Plus**；结构版本化见 [Masterdata-ORM-7.3体系.md](Masterdata-ORM-7.3体系.md)。

---

## 2. 老继承体系

`
MasterDataEntity (createTime, updateTime)
  +-- MasterDataIdEntity (+ id)
        +-- MasterDataTreeEntity (+ parentId, level, leafPath, leaf)
        +-- MasterDataCodeEntity (+ code)
  +-- MultiPlatformEntity (+ appId)
        +-- MultiPlatformIdEntity (+ id)
              +-- MultiPlatformTreeEntity (+ tree)
`

| 老基类 | 新增字段 | 典型实体 |
|--------|----------|----------|
| MasterDataIdEntity | id | BaseUser, BaseRole |
| MasterDataTreeEntity | 树字段 | BaseDic, BaseOrg, BaseMenu |
| MasterDataCodeEntity | code | PushMessageBody |
| MultiPlatformEntity | ppId | 多应用表 |
| MultiPlatformTreeEntity | ppId + 树 | 多平台树 |

| 老实体基类 | Service | ServiceImpl | Controller |
|------------|---------|-------------|------------|
| MasterDataIdEntity | IMasterDataService | MasterDataServiceImpl | MasterDataCollection |
| MasterDataTreeEntity | IMasterDataTreeService | MasterDataTreeServiceImpl | MasterDataTreeCollection |
| MasterDataCodeEntity | ~~IMasterDataCodeService~~ | ~~MasterDataCodeServiceImpl~~ | MasterDataCollection |
| MultiPlatform* | ~~IMultiPlatform*~~ | ~~MultiPlatform*Impl~~ | 同上 |

---

## 3. 新体系：MasterDataEntity

包：com.jbm.framework.masterdata.usage.entity.MasterDataEntity

| 字段 | 说明 |
|------|------|
| id | 通用列；基类 **无** @TableId，子类定义业务主键 |
| code, ppId | 编码 / 多应用 |
| parentId, level, leafPath | 树 |
| leaf | 基类已 exist=false |
| createTime, updateTime | 审计填充 |
| extendData, extendQuery | 动态字段，见 [动态字段使用方案](动态字段使用方案.md) |

Service 链（泛型统一 Entity extends MasterDataEntity）：

`
IMasterDataService -> IMasterDataTreeService
MasterDataServiceImpl -> MasterDataTreeServiceImpl
MasterDataCollection -> MasterDataTreeCollection
`

---

## 4. 老基类 -> 新写法

| 迁移前 extends | 迁移后 | 子类收紧 |
|----------------|--------|----------|
| MasterDataIdEntity | MasterDataEntity | 无列则 exist=false：code,ppId,树 |
| MasterDataTreeEntity | MasterDataEntity | 排除无列 code,ppId |
| MasterDataCodeEntity | MasterDataEntity | 排除 ppId,树 |
| MultiPlatformEntity | MasterDataEntity | 排除 code,树 |
| MultiPlatformTreeEntity | MasterDataEntity | 排除 code |

---

## 5. 实体迁移步骤

1. public class Xxx extends MasterDataEntity
2. 对照 Liquibase/实表，对基类多余字段加 @TableField(exist = false)
3. 主键：子类 @TableId；非 id 列或 UUID 见 WebhookTask
4. extend_data：@TableName(autoResultMap = true)

| 分型 | 建议 exist=false | 保留 |
|------|------------------|------|
| 普通扁平 | code, appId, 树 | 主键、审计、业务列 |
| 树形 | code, appId（无列时） | 树字段 |
| 多平台扁平 | code, 树 | appId |
| 多平台树 | code | appId + 树 |
| 编码主数据 | appId, 树 | code |

参考：jbm-cluster-api-basic 中 WebhookTask、BaseDic、BaseOrg。

---

## 6. Service / Controller

- 删除 IMultiPlatform*、IMasterDataCode* 引用
- TreeBusiness<E extends MasterDataEntity, ...>
- 先改 api 模块实体，再编译 mysql/center 等模块
- 树能力仍用 MasterDataTreeServiceImpl + MasterDataTreeCollection

---

## 7. 代码生成器

EntityType.MasterData / MasterDataTree 不变；生成实体须 extends MasterDataEntity。

---

## 8. 检查清单

- [ ] extends MasterDataEntity，无旧基类 import
- [ ] exist=false 与表结构一致
- [ ] 单一 @TableId
- [ ] 无已删 Service 接口
- [ ] mvn -pl 模块 -am compile

---

## 9. 相关文档

- [Masterdata-ORM-7.3体系.md](Masterdata-ORM-7.3体系.md) — Liquibase + MP
- [动态字段使用方案.md](动态字段使用方案.md)
- [项目架构.md](项目架构.md) — 4.3 MasterDataEntity 设计意图

*7.3.0-SNAPSHOT*
