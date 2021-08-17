package com.jbm.cluster.logs.service;

import com.jbm.cluster.logs.entity.GatewayLogs;
import com.jbm.cluster.logs.form.GatewayLogsForm;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2021-05-06 16:55
 **/
public interface GatewayLogsService extends BaseDataService<GatewayLogs> {


    DataPaging<GatewayLogs> findLogs(GatewayLogsForm gatewayLogsForm);
}
