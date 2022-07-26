package com.jbm.cluster.api.entitys.gateway;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;

/**
 * @author wesley.zhang
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("网关API限流")
@TableName("gateway_rate_limit_api")
public class GatewayRateLimitApi extends MasterDataIdEntity {
    private static final long serialVersionUID = 1L;
    /**
     * 限制数量
     */
    private Long policyId;
    /**
     * 时间间隔(秒)
     */
    private Long apiId;
}
