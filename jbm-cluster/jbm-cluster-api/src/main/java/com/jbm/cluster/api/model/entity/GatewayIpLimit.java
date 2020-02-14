package com.jbm.cluster.api.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.bean.MasterDataEntity;
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
@TableName("gateway_ip_limit")
public class GatewayIpLimit extends MasterDataEntity {

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

    private static final long serialVersionUID = 1L;
}
