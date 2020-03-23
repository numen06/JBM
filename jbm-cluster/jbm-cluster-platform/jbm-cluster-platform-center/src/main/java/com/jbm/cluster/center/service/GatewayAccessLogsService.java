package com.jbm.cluster.center.service;

import com.jbm.cluster.api.model.entity.GatewayAccessLogs;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.usage.paging.DataPaging;

/**
 * 网关访问日志
 * @author wesley.zhang
 */
public interface GatewayAccessLogsService {
    /**
     * 分页查询
     *
     * @param pageRequestBody
     * @return
     */
    DataPaging<GatewayAccessLogs> findListPage(PageRequestBody<GatewayAccessLogs> pageRequestBody);
}
