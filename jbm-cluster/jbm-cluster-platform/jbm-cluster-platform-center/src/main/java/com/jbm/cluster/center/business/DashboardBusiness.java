package com.jbm.cluster.center.business;

import com.jbm.cluster.api.model.dashboard.DashboardOverview;

/**
 * 仪表盘聚合业务
 */
public interface DashboardBusiness {

  /**
   * 构建当前登录用户的仪表盘概览（指标按权限裁剪）
   */
  DashboardOverview buildOverview();
}
