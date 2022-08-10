package com.jbm.cluster.api.entitys.gateway;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author wesley.zhang
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("网关限流策略")
@TableName("gateway_rate_limit")
public class GatewayRateLimit extends MasterDataEntity {
    private static final long serialVersionUID = 1L;
    @Id
    @TableId(type = IdType.ASSIGN_ID)
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
}
