package com.jbm.cluster.push.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpResponse;
import com.jbm.cluster.api.entitys.message.WebhookEventConfig;
import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.common.basic.module.JbmRequestTemplate;
import com.jbm.cluster.push.form.WebhookTaskForm;
import com.jbm.cluster.push.service.WebhookEventConfigService;
import com.jbm.cluster.push.service.WebhookTaskService;
import com.jbm.framework.service.mybatis.MultiPlatformServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-08-30 16:36:49
 */
@Service
@Slf4j
public class WebhookTaskServiceImpl extends MultiPlatformServiceImpl<WebhookTask> implements WebhookTaskService {


    @Autowired
    private JbmRequestTemplate jbmRequestTemplate;
    @Autowired
    private WebhookEventConfigService webhookEventConfigService;

    @Override
    public void sendEvent(WebhookTaskForm webhookTaskForm) {
        List<WebhookEventConfig> webhookEventConfigs = webhookEventConfigService.selectByEventCode(webhookTaskForm.getWebhookEventConfig().getBusinessEventCode());
        if (CollUtil.isEmpty(webhookEventConfigs)) {
            return;
        }
        webhookEventConfigs.forEach(webhookEventConfig -> {
            sendEvent(webhookEventConfig, webhookTaskForm.getWebhookTask());
        });
    }

    public void sendEvent(WebhookEventConfig webhookEventConfig, WebhookTask sourceWebhookTask) {
        AtomicBoolean ok = new AtomicBoolean(true);
        WebhookTask webhookTask = new WebhookTask();
        webhookTask.setRequest(sourceWebhookTask.getRequest());
        //初始化一个方法
        webhookTask.setEventId(webhookEventConfig.getEventId());
        webhookTask.setRetryNumber(0);
        this.saveEntity(webhookTask);
        while (ok.get()) {
            try {
                HttpResponse response = jbmRequestTemplate.request(webhookEventConfig.getUrl(), webhookEventConfig.getMethodType(), webhookTask.getRequest());
                webhookTask.setResponse(response.body());
                webhookTask.setHttpStatus(response.getStatus());
                this.saveEntity(webhookTask);
                ok.set(false);
            } catch (Exception e) {
                ThreadUtil.safeSleep(500);
                webhookTask.setRetryNumber(webhookTask.getRetryNumber() + 1);
                log.error("执行远程业务事件错误", e);
            }
        }
    }
}