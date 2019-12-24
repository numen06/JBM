package com.jbm.cluster.system.mapper;

import com.jbm.cluster.api.model.entity.GatewayAccessLogs;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

/**
 * @author liuyadu
 */
@Repository
public interface GatewayLogsMapper extends BaseMapper<GatewayAccessLogs> {
}
