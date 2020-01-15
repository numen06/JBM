package com.jbm.cluster.system.service;

import com.jbm.cluster.api.model.entity.GatewayAccessLogs;
import com.jbm.framework.usage.form.JsonRequestBody;
import com.jbm.framework.usage.paging.DataPaging;

/**
 * 网关访问日志
 * @author wesley.zhang
 */
public interface GatewayAccessLogsService {
    /**
     * 分页查询
     *
     * @param jsonRequestBody
     * @return
     */
    DataPaging<GatewayAccessLogs> findListPage(JsonRequestBody jsonRequestBody);
}
