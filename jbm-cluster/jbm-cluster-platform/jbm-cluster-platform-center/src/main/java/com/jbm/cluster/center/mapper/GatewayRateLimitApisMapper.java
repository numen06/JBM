package com.jbm.cluster.center.mapper;

import com.jbm.cluster.api.model.RateLimitApi;
import com.jbm.cluster.api.model.entity.GatewayRateLimitApi;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author wesley.zhang
 */
@Repository
public interface GatewayRateLimitApisMapper extends BaseMapper<GatewayRateLimitApi> {

    List<RateLimitApi> selectRateLimitApi();

}
