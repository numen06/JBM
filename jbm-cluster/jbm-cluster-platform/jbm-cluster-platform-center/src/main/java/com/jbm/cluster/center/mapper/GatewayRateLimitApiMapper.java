package com.jbm.cluster.center.mapper;

import com.jbm.cluster.api.entitys.gateway.GatewayRateLimitApi;
import com.jbm.cluster.api.model.RateLimitApi;
import com.jbm.framework.masterdata.annotation.MapperRepository;
import com.jbm.framework.masterdata.mapper.SuperMapper;

import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2020-02-25 03:47:52
 */
@MapperRepository
public interface GatewayRateLimitApiMapper extends SuperMapper<GatewayRateLimitApi> {
    List<RateLimitApi> selectRateLimitApi();
}
