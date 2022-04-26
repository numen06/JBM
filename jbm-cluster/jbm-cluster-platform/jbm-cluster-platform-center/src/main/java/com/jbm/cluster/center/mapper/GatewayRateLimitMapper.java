package com.jbm.cluster.center.mapper;

import com.jbm.cluster.api.entitys.gateway.GatewayRateLimit;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import org.springframework.stereotype.Repository;

@Repository
public interface GatewayRateLimitMapper extends SuperMapper<GatewayRateLimit> {
}
