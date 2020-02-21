package com.jbm.cluster.center.mapper;

import com.jbm.cluster.api.model.entity.GatewayAccessLogs;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import org.springframework.stereotype.Repository;

/**
 * @author liuyadu
 */
@Repository
public interface GatewayLogsMapper extends SuperMapper<GatewayAccessLogs> {
}
