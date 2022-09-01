package com.jbm.cluster.push.controller;

import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.push.service.WebhookTaskService;
import com.jbm.framework.mvc.web.MultiPlatformCollection;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-08-30 16:36:49
 */
@Api(tags = "Web反向推送开放接口")
@RestController
@RequestMapping("/webhookTask")
public class WebhookTaskController extends MultiPlatformCollection<WebhookTask, WebhookTaskService> {
}
