package com.jbm.cluster.center.business.impl;

import cn.hutool.core.bean.BeanUtil;
import com.jbm.cluster.api.entitys.gateway.GatewayRateLimit;
import com.jbm.cluster.api.form.GatewayRateLimitForm;
import com.jbm.cluster.center.business.GatewayRateLimitBusiness;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.common.mysql.service.impl.GatewayRateLimitServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class GatewayRateLimitBusinessImpl extends GatewayRateLimitServiceImpl implements GatewayRateLimitBusiness {

    @Autowired
    private JbmClusterTemplate jbmClusterTemplate;

    @Override
    public Long addRateLimitWithGatewayRefresh(GatewayRateLimitForm form) {
        GatewayRateLimit rateLimit = BeanUtil.toBean(form, GatewayRateLimit.class);
        GatewayRateLimit result = addRateLimitPolicy(rateLimit);
        jbmClusterTemplate.refreshGateway();
        return result != null ? result.getPolicyId() : null;
    }

    @Override
    public void updateRateLimitWithGatewayRefresh(GatewayRateLimitForm form) {
        GatewayRateLimit rateLimit = BeanUtil.toBean(form, GatewayRateLimit.class);
        updateRateLimitPolicy(rateLimit);
        jbmClusterTemplate.refreshGateway();
    }

    @Override
    public void removeRateLimitWithGatewayRefresh(Long policyId) {
        removeRateLimitPolicy(policyId);
        jbmClusterTemplate.refreshGateway();
    }

    @Override
    public void addRateLimitApisWithGatewayRefresh(Long policyId, String[] apiIds) {
        addRateLimitApis(policyId, apiIds);
        jbmClusterTemplate.refreshGateway();
    }
}