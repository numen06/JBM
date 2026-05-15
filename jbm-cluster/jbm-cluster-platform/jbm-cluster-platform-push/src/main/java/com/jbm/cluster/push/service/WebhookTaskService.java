package com.jbm.cluster.push.service;

import com.jbm.cluster.api.entitys.message.WebhookEventConfig;
import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.push.form.WebhookTaskForm;
import com.jbm.cluster.push.result.WebhookTaskResult;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.usage.paging.DataPaging;

import java.util.List;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-08-30 16:36:49
 */
public interface WebhookTaskService extends IMasterDataService<WebhookTask> {


    boolean clearTasks();

    List<WebhookEventConfig> getEnableEventConfigs(WebhookTaskForm webhookTaskForm);

    void sendBusinessEvent(WebhookTaskForm webhookTaskForm);

    WebhookTask selectByTaskId(String taskId);

    void sendBusinessEvent(String eventId);

    void sendBusinessEvent(WebhookTask webhookTask);

    void retryEventTask(String taskId);

    DataPaging<WebhookTaskResult> selectWebhookTasks(WebhookTaskForm webhookTaskForm);
}
