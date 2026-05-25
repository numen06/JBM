package com.jbm.cluster.api.model.dashboard;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

/**
 * 仪表盘平台级计数快照（用于 Redis 缓存，响应时仍按权限裁剪字段）
 */
@Data
@ApiModel("仪表盘平台计数快照")
public class DashboardPlatformCounts implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long usersTotal;
    private Long appCount;
    private Long orgCount;
    private Long roleCount;
    private Long authorityResourceCount;
    private Long apiCount;
    private Long apiKeyCount;
}
