package com.jbm.cluster.api.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;

/**
 * 网关动态路由
 *
 * @author: wesley.zhang
 * @date: 2018/10/24 16:21
 * @description:
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("gateway_route")
public class GatewayRoute extends MasterDataEntity {


    public Long getRouteId() {
        return this.getId();
    }

    public void setRouteId(Long routeId) {
        this.setId(routeId);
    }


    /**
     * 路由名称
     */
    private String routeName;

    /**
     * 路径
     */
    private String path;

    /**
     * 服务ID
     */
    private String serviceId;

    /**
     * 完整地址
     */
    private String url;

    /**
     * 忽略前缀
     */
    private Integer stripPrefix;

    /**
     * 0-不重试 1-重试
     */
    private Integer retryable;

    /**
     * 状态:0-无效 1-有效
     */
    private Integer status;

    /**
     * 保留数据0-否 1-是 不允许删除
     */
    private Integer isPersist;

    /**
     * 路由说明
     */
    private String routeDesc;
}
