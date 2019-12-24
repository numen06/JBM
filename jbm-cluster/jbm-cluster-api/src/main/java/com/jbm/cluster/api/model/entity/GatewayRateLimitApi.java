package com.jbm.cluster.api.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.bean.MasterDataEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author liuyadu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("gateway_rate_limit_api")
public class GatewayRateLimitApi extends MasterDataEntity {
    /**
     * 限制数量
     */
    private Long policyId;

    /**
     * 时间间隔(秒)
     */
    private Long apiId;


    private static final long serialVersionUID = 1L;
}
