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

    void sendBusinessEvent(WebhookTaskForm webhookTaskForm);

    WebhookTask selectByTaskId(String taskId);

    void sendBusinessEvent(String eventId);

    void sendBusinessEvent(WebhookTask webhookTask);

    void retryEventTask(String taskId);

    DataPaging<WebhookTaskReslut> selectWebhookTasks(WebhookTaskForm webhookTaskForm);
}
