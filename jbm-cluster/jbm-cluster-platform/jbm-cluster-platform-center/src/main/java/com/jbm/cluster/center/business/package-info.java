/**
 * 平台侧业务编排（Masterdata 代码生成约定）。
 * <p>
 * 分层与命名：
 * <ul>
 *   <li>{@code jbm-cluster-common-mysql}：{@code XxxMapper} / {@code XxxService} / {@code XxxServiceImpl}（ORM + 主数据 CRUD）</li>
 *   <li>{@code center.business}：{@code XxxBusiness} / {@code XxxBusinessImpl}（平台编排，继承对应 ServiceImpl，{@code @Primary}）</li>
 *   <li>{@code center.controller}：{@code MasterDataCollection<Entity, XxxBusiness>} 或 Feign 入口</li>
 * </ul>
 * 通用 CRUD 勿放在本包；复杂实体在实体类上用 {@code @IgnoreGeneate} 保护已扩展的 Service。
 */
package com.jbm.cluster.center.business;
