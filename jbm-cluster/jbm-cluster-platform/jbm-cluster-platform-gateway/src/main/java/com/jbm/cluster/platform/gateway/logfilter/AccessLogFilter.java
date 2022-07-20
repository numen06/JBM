package com.jbm.cluster.platform.gateway.logfilter;

import com.jbm.cluster.api.model.gateway.GatewayLogInfo;

import java.util.Map;

public interface AccessLogFilter {

    void filter(GatewayLogInfo gatewayLogInfo, Map<String, String> headers);
}
