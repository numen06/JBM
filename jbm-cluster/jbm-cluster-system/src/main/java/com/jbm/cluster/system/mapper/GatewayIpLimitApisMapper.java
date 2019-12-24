package com.jbm.cluster.system.mapper;

import com.jbm.cluster.api.model.IpLimitApi;
import com.jbm.cluster.api.model.entity.GatewayIpLimitApi;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author liuyadu
 */
@Repository
public interface GatewayIpLimitApisMapper extends BaseMapper<GatewayIpLimitApi> {

    List<IpLimitApi> selectIpLimitApi(@Param("policyType") int policyType);
}
