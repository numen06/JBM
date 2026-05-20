package com.jbm.cluster.center.business;

import com.jbm.cluster.api.form.GatewayRateLimitForm;
import com.jbm.cluster.common.mysql.service.GatewayRateLimitService;

public interface GatewayRateLimitBusiness extends GatewayRateLimitService {

    Long addRateLimitWithGatewayRefresh(GatewayRateLimitForm form);

    void updateRateLimitWithGatewayRefresh(GatewayRateLimitForm form);

    void removeRateLimitWithGatewayRefresh(Long policyId);

    void addRateLimitApisWithGatewayRefresh(Long policyId, String[] apiIds);
}