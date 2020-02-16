package com.jbm.cluster.center.mapper;

import com.jbm.cluster.api.model.entity.GatewayAccessLogs;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

/**
 * @author wesley.zhang
 */
@Repository
public interface GatewayLogsMapper extends BaseMapper<GatewayAccessLogs> {
}
