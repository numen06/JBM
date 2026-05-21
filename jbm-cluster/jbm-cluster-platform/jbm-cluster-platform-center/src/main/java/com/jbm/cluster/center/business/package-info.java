/**
 * 平台侧业务编排（Masterdata 代码生成约定）。
 * <p>
 * 完整规范：<b>docs/CBSM-standard.md</b>（分层职责、方法名事务 AOP、{@code this} 拆解）。
 * <ul>
 *   <li>{@code jbm-cluster-common-mysql}：{@code XxxMapper} / {@code XxxService} / {@code XxxServiceImpl}</li>
 *   <li>{@code center.business}：{@code XxxBusiness} / {@code XxxBusinessImpl}（组合 {@code XxxService}，禁止 {@code extends ServiceImpl}）</li>
 *   <li>{@code center.controller}：查询走 Service，编排走 Business</li>
 * </ul>
 */
package com.jbm.cluster.center.business;
