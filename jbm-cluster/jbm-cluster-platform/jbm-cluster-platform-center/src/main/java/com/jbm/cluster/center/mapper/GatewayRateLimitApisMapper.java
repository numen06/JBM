package com.jbm.cluster.center.mapper;

import com.jbm.cluster.api.model.RateLimitApi;
import com.jbm.cluster.api.model.entity.GatewayRateLimitApi;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author liuyadu
 */
@Repository
public interface GatewayRateLimitApisMapper extends SuperMapper<GatewayRateLimitApi> {

    List<RateLimitApi> selectRateLimitApi();

}
