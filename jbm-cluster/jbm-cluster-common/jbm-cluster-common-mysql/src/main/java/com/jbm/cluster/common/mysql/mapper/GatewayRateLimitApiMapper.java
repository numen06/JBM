package com.jbm.cluster.common.mysql.mapper;

import com.jbm.cluster.api.entitys.gateway.GatewayRateLimitApi;
import com.jbm.cluster.api.model.api.ApiControlCount;
import com.jbm.cluster.api.model.RateLimitApi;
import com.jbm.framework.masterdata.annotation.MapperRepository;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2020-02-25 03:47:52
 */
@MapperRepository
public interface GatewayRateLimitApiMapper extends SuperMapper<GatewayRateLimitApi> {
    List<RateLimitApi> selectRateLimitApi();

    List<ApiControlCount> countRateLimitByApiIds(@Param("apiIds") List<Long> apiIds);
}
