package com.jbm.cluster.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jbm.cluster.api.model.entity.GatewayAccessLogs;

/**
 * 网关访问日志
 * @author liuyadu
 */
public interface GatewayAccessLogsService {
    /**
     * 分页查询
     *
     * @param pageForm
     * @return
     */
    IPage<GatewayAccessLogs> findListPage(PageForm pageForm);
}
