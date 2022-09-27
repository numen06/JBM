package com.jbm.cluster.push.form;

import com.jbm.cluster.api.entitys.message.WebhookEventConfig;
import com.jbm.cluster.api.entitys.message.WebhookTask;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@ApiModel("Web反向推送")
public class WebhookTaskForm {

    private WebhookTask webhookTask;
    private WebhookEventConfig webhookEventConfig;
}
