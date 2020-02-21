package com.jbm.cluster.center.mapper;

import com.jbm.cluster.api.model.IpLimitApi;
import com.jbm.cluster.api.model.entity.GatewayIpLimitApi;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author liuyadu
 */
@Repository
public interface GatewayIpLimitApisMapper extends SuperMapper<GatewayIpLimitApi> {

    List<IpLimitApi> selectIpLimitApi(@Param("policyType") int policyType);
}
