package com.jbm.cluster.center.business;

import com.jbm.cluster.api.form.GatewayIpLimitForm;
import com.jbm.cluster.common.mysql.service.GatewayIpLimitService;

public interface GatewayIpLimitBusiness extends GatewayIpLimitService {

    Long addIpLimitWithGatewayRefresh(GatewayIpLimitForm form);

    void updateIpLimitWithGatewayRefresh(GatewayIpLimitForm form);

    void removeIpLimitWithGatewayRefresh(Long policyId);

    void addIpLimitApisWithGatewayRefresh(Long policyId, String[] apiIds);
}