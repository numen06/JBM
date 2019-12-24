package com.jbm.cluster.api.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
@TableName("gateway_rate_limit")
public class GatewayRateLimit extends MasterDataEntity {

    @TableId(type = IdType.ID_WORKER)
    private Long policyId;

    /**
     * 策略名称
     */
    private String policyName;

    /**
     * 限流规则类型:url,origin,user
     */
    private String policyType;

    /**
     * 限制数
     */
    private Long limitQuota;

    /**
     * 单位时间:seconds-秒,minutes-分钟,hours-小时,days-天
     */
    private String intervalUnit;

    private static final long serialVersionUID = 1L;
}
