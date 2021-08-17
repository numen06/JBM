package com.jbm.cluster.logs.form;

import com.jbm.cluster.logs.entity.GatewayLogs;
import com.jbm.framework.usage.paging.PageForm;
import lombok.Data;

import java.util.Date;

@Data
public class GatewayLogsForm {

    private GatewayLogs gatewayLogs;

    private PageForm pageForm;

    private Date beginTime;

    private Date endTime;
}
