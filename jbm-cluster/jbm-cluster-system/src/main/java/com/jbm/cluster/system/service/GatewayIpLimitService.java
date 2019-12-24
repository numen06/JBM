package com.jbm.cluster.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jbm.cluster.api.model.IpLimitApi;
import com.jbm.cluster.api.model.entity.GatewayIpLimit;
import com.jbm.cluster.api.model.entity.GatewayIpLimitApi;
import com.jbm.framework.masterdata.service.IMasterDataService;

import java.util.List;

/**
 * 网关IP访问控制
 *
 * @author liuyadu
 */
public interface GatewayIpLimitService extends IMasterDataService<GatewayIpLimit> {
    /**
     * 分页查询
     *
     * @param pageForm
     * @return
     */
    IPage<GatewayIpLimit> findListPage(PageForm pageForm);

    /**
     * 查询白名单
     *
     * @return
     */
    List<IpLimitApi> findBlackList();

    /**
     * 查询黑名单
     *
     * @return
     */
    List<IpLimitApi> findWhiteList();

    /**
     * 查询策略已绑定API列表
     *
     * @return
     */
    List<GatewayIpLimitApi> findIpLimitApiList(Long policyId);

    /**
     * 获取IP限制策略
     *
     * @param policyId
     * @return
     */
    GatewayIpLimit getIpLimitPolicy(Long policyId);

    /**
     * 添加IP限制策略
     *
     * @param policy
     * @return
     */
    GatewayIpLimit addIpLimitPolicy(GatewayIpLimit policy);

    /**
     * 更新IP限制策略
     *
     * @param policy
     */
    GatewayIpLimit updateIpLimitPolicy(GatewayIpLimit policy);

    /**
     * 删除IP限制策略
     *
     * @param policyId
     */
    void removeIpLimitPolicy(Long policyId);

    /**
     * 绑定API, 一个API只能绑定一个策略
     *
     * @param policyId
     * @param apis
     */
    void addIpLimitApis(Long policyId, String... apis);

    /**
     * 清空绑定的API
     *
     * @param policyId
     */
    void clearIpLimitApisByPolicyId(Long policyId);

    /**
     * API解除所有策略
     *
     * @param apiId
     */
    void clearIpLimitApisByApiId(Long apiId);

}
