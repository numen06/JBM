package com.jbm.cluster.push.service;

import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.push.form.WebhookTaskForm;
import com.jbm.framework.masterdata.service.IMultiPlatformService;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-08-30 16:36:49
 */
public interface WebhookTaskService extends IMultiPlatformService<WebhookTask> {


    void sendEvent(WebhookTaskForm webhookTaskForm);
}
