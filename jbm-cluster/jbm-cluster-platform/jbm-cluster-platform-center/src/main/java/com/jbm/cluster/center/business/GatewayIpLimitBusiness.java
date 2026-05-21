package com.jbm.cluster.center.business;

import com.jbm.cluster.api.form.GatewayIpLimitForm;

public interface GatewayIpLimitBusiness {

    Long addIpLimitWithGatewayRefresh(GatewayIpLimitForm form);

    void updateIpLimitWithGatewayRefresh(GatewayIpLimitForm form);

    void removeIpLimitWithGatewayRefresh(Long policyId);

    void addIpLimitApisWithGatewayRefresh(Long policyId, String[] apiIds);
}
