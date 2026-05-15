/**
 * 平台侧业务编排扩展包。
 * <p>
 * 数据访问与通用 CRUD 已下沉至 {@code jbm-cluster-common-mysql}（{@code com.jbm.cluster.common.mysql}）。
 * 若需将部分流程从公共 Service 中进一步拆分至平台专属逻辑，可在此增加 *Business / *BusinessImpl。
 */
package com.jbm.cluster.center.business;
