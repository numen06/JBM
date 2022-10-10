package com.jbm.cluster.push.form;

import com.jbm.cluster.api.entitys.message.WebhookEventConfig;
import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.framework.usage.paging.PageForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@ApiModel("Web反向推送")
public class WebhookTaskForm {

    @ApiModelProperty(value = "事件任务")
    private WebhookTask webhookTask;
    @ApiModelProperty(value = "事件配置")
    private WebhookEventConfig webhookEventConfig;
    @ApiModelProperty(value = "开始时间")
    private Date beginTime;
    @ApiModelProperty(value = "结束时间")
    private Date endTime;
    @ApiModelProperty(value = "分页对象")
    private PageForm pageForm;
}
