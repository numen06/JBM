package com.jbm.cluster.logs.form;

import com.jbm.cluster.logs.entity.GatewayLogs;
import com.jbm.framework.usage.paging.PageForm;
import lombok.Data;

@Data
public class GatewayLogsForm {

    private GatewayLogs gatewayLogs;

    private PageForm pageForm;
}
