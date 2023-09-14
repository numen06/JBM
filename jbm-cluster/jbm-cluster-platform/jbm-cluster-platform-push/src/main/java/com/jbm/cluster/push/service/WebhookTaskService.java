package com.jbm.cluster.push.service;

import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.push.form.WebhookTaskForm;
import com.jbm.cluster.push.result.WebhookTaskReslut;
import com.jbm.framework.masterdata.service.IMultiPlatformService;
import com.jbm.framework.usage.paging.DataPaging;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-08-30 16:36:49
 */
public interface WebhookTaskService extends IMultiPlatformService<WebhookTask> {


    boolean clearTasks();

    void sendEvent(WebhookTaskForm webhookTaskForm);

    WebhookTask selectByTaskId(String taskId);

    void sendEvent(String eventId);

    void sendEvent(WebhookTask webhookTask);

    void retryEventTask(String taskId);

    DataPaging<WebhookTaskReslut> selectWebhookTasks(WebhookTaskForm webhookTaskForm);
}
