package com.jbm.cluster.push.result;

import com.jbm.cluster.api.entitys.message.WebhookEventConfig;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@ApiModel("Web反向推送返回")
public class WebhookTaskReslut extends WebhookEventConfig {

    @ApiModelProperty(value = "任务ID")
    private String taskId;
    @ApiModelProperty("事件状态")
    private Integer httpStatus;
    @ApiModelProperty("重试次数")
    private Integer retryNumber;
}
