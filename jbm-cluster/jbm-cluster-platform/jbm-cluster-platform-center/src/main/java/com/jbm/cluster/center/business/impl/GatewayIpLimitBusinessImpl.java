package com.jbm.cluster.center.business.impl;

import cn.hutool.core.bean.BeanUtil;
import com.jbm.cluster.api.entitys.gateway.GatewayIpLimit;
import com.jbm.cluster.api.form.GatewayIpLimitForm;
import com.jbm.cluster.center.business.GatewayIpLimitBusiness;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.common.mysql.service.impl.GatewayIpLimitServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class GatewayIpLimitBusinessImpl extends GatewayIpLimitServiceImpl implements GatewayIpLimitBusiness {

    @Autowired
    private JbmClusterTemplate jbmClusterTemplate;

    @Override
    public Long addIpLimitWithGatewayRefresh(GatewayIpLimitForm form) {
        GatewayIpLimit ipLimit = BeanUtil.toBean(form, GatewayIpLimit.class);
        GatewayIpLimit result = addIpLimitPolicy(ipLimit);
        jbmClusterTemplate.refreshGateway();
        return result != null ? result.getPolicyId() : null;
    }

    @Override
    public void updateIpLimitWithGatewayRefresh(GatewayIpLimitForm form) {
        GatewayIpLimit ipLimit = BeanUtil.toBean(form, GatewayIpLimit.class);
        updateIpLimitPolicy(ipLimit);
        jbmClusterTemplate.refreshGateway();
    }

    @Override
    public void removeIpLimitWithGatewayRefresh(Long policyId) {
        removeIpLimitPolicy(policyId);
        jbmClusterTemplate.refreshGateway();
    }

    @Override
    public void addIpLimitApisWithGatewayRefresh(Long policyId, String[] apiIds) {
        addIpLimitApis(policyId, apiIds);
        jbmClusterTemplate.refreshGateway();
    }
}