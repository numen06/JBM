package com.jbm.cluster.push.handler;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.entitys.message.WebhookEventConfig;
import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.api.model.event.JbmClusterBusinessEventBean;
import com.jbm.cluster.api.model.event.JbmClusterBusinessEventResource;
import com.jbm.cluster.push.form.WebhookTaskForm;
import com.jbm.cluster.push.service.WebhookEventConfigService;
import com.jbm.cluster.push.service.WebhookTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

@Configuration
public class BusinessEventHandler {


    @Autowired
    private WebhookEventConfigService webhookEventConfigService;


    @Autowired
    private WebhookTaskService webhookTaskService;

    @Bean
    public Function<Flux<Message<JbmClusterBusinessEventResource>>, Mono<Void>> businessEventResource() {
        return flux -> flux.map(message -> {
            this.receive(message.getPayload());
            return message;
        }).then();
    }

    @Bean
    public Function<Flux<Message<JbmClusterBusinessEventBean>>, Mono<Void>> businessEvent() {
        return flux -> flux.map(message -> {
            this.sendEvent(message.getPayload());
            return message;
        }).then();
    }

    public void sendEvent(JbmClusterBusinessEventBean jbmClusterBusinessEventBean) {
        WebhookTaskForm webhookTaskForm = beanToWebHook(jbmClusterBusinessEventBean);
        webhookTaskService.sendEvent(webhookTaskForm);
    }

    public void receive(JbmClusterBusinessEventResource jbmClusterBusinessEventResource) {
        List<JbmClusterBusinessEventBean> jbmClusterBusinessEventBeans = jbmClusterBusinessEventResource.getJbmClusterBusinessEventBeans();
        jbmClusterBusinessEventBeans.forEach(jbmClusterBusinessEventBean -> {
            WebhookTaskForm webhookTaskForm = beanToWebHook(jbmClusterBusinessEventBean);
            WebhookEventConfig webhookEventConfig = webhookEventConfigService.selectByCodeUrl(webhookTaskForm.getWebhookEventConfig().getBusinessEventCode(), webhookTaskForm.getWebhookEventConfig().getUrl());
            if (ObjectUtil.isNotEmpty(webhookEventConfig)) {
                webhookTaskForm.getWebhookEventConfig().setEventId(webhookEventConfig.getEventId());
            }
            webhookEventConfigService.saveEntity(webhookTaskForm.getWebhookEventConfig());
        });
    }

    public WebhookTaskForm beanToWebHook(JbmClusterBusinessEventBean jbmClusterBusinessEventBean) {
        WebhookTaskForm webhookTaskForm = new WebhookTaskForm();
        WebhookEventConfig webhookEventConfig = new WebhookEventConfig();
        BeanUtil.copyProperties(jbmClusterBusinessEventBean, webhookEventConfig);
        webhookEventConfig.setEventName(jbmClusterBusinessEventBean.getEventName());
        webhookEventConfig.setBusinessEventCode(jbmClusterBusinessEventBean.getEventCode());
        webhookEventConfig.setUrl(jbmClusterBusinessEventBean.getUrl());
        webhookEventConfig.setEventGroup(jbmClusterBusinessEventBean.getEventGroup());
        webhookEventConfig.setMethodType(jbmClusterBusinessEventBean.getMethodType());
        WebhookTask webhookTask = new WebhookTask();
        webhookTask.setEventId(webhookEventConfig.getEventId());
        webhookTask.setRequest(jbmClusterBusinessEventBean.getEventBody());
        webhookTaskForm.setWebhookTask(webhookTask);
        webhookTaskForm.setWebhookEventConfig(webhookEventConfig);
        return webhookTaskForm;
    }


}
