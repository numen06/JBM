package com.jbm.cluster.api.entitys.gateway;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
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
@TableName("gateway_ip_limit")
public class GatewayIpLimit extends MasterDataEntity {
    private static final long serialVersionUID = 1L;
    /**
     * 策略ID
     */
    @Id
    @TableId(type = IdType.ID_WORKER)
    private Long policyId;
    /**
     * 策略名称
     */
    private String policyName;
    /**
     * 策略类型:0-拒绝/黑名单 1-允许/白名单
     */
    private Integer policyType;
    /**
     * ip地址/IP段:多个用隔开;最多10个
     */
    private String ipAddress;
}
