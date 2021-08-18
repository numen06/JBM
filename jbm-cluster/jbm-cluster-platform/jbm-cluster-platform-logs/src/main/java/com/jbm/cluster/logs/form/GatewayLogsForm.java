package com.jbm.cluster.logs.form;

import com.jbm.cluster.logs.entity.GatewayLogs;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel(value = "日志查询表单")
public class GatewayLogsForm {
    @ApiModelProperty(value = "日志查询实体")
    private GatewayLogs gatewayLogs;
    @ApiModelProperty(value = "分页对象")
    private PageForm pageForm;
    @ApiModelProperty(value = "开始时间")
    private Date beginTime;
    @ApiModelProperty(value = "结束时间")
    private Date endTime;
}
