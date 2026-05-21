package com.jbm.cluster.center.business;

import com.jbm.cluster.api.form.GatewayRateLimitForm;

public interface GatewayRateLimitBusiness {

    Long addRateLimitWithGatewayRefresh(GatewayRateLimitForm form);

    void updateRateLimitWithGatewayRefresh(GatewayRateLimitForm form);

    void removeRateLimitWithGatewayRefresh(Long policyId);

    void addRateLimitApisWithGatewayRefresh(Long policyId, String[] apiIds);
}
