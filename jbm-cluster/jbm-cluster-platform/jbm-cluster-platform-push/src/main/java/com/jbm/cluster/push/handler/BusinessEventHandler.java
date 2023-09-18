package com.jbm.cluster.push.handler;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
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

    /**
     * 接受集群事件推送
     * @param jbmClusterBusinessEventBean
     */
    public void sendEvent(JbmClusterBusinessEventBean jbmClusterBusinessEventBean) {
        WebhookTaskForm webhookTaskForm = beanToWebHook(jbmClusterBusinessEventBean);
        webhookTaskService.sendEvent(webhookTaskForm);
    }

    /**
     * 接受应用程序推过来的事件
     *
     * @param jbmClusterBusinessEventResource
     */
    public void receive(JbmClusterBusinessEventResource jbmClusterBusinessEventResource) {
        List<JbmClusterBusinessEventBean> jbmClusterBusinessEventBeans = jbmClusterBusinessEventResource.getJbmClusterBusinessEventBeans();
        final String batchTime = DateUtil.now();
        jbmClusterBusinessEventBeans.forEach(jbmClusterBusinessEventBean -> {
            WebhookTaskForm webhookTaskForm = beanToWebHook(jbmClusterBusinessEventBean);
            WebhookEventConfig webhookEventConfig = webhookEventConfigService.selectByCodeUrl(webhookTaskForm.getWebhookEventConfig().getBusinessEventCode(), webhookTaskForm.getWebhookEventConfig().getUrl());
            if (ObjectUtil.isNotEmpty(webhookEventConfig)) {
                webhookTaskForm.getWebhookEventConfig().setEventId(webhookEventConfig.getEventId());
            }
            webhookTaskForm.getWebhookEventConfig().setBatchTime(batchTime);
            webhookEventConfigService.saveEntity(webhookTaskForm.getWebhookEventConfig());
        });
        webhookEventConfigService.deleteOldBatch(jbmClusterBusinessEventResource.getServiceId(), batchTime);
    }

    /***
     * 将扫描数据转为成传输数据
     * @param jbmClusterBusinessEventBean
     * @return
     */
    public WebhookTaskForm beanToWebHook(JbmClusterBusinessEventBean jbmClusterBusinessEventBean) {
        WebhookTaskForm webhookTaskForm = new WebhookTaskForm();
        WebhookEventConfig webhookEventConfig = new WebhookEventConfig();
        BeanUtil.copyProperties(jbmClusterBusinessEventBean, webhookEventConfig);
        webhookEventConfig.setEventName(jbmClusterBusinessEventBean.getEventName());
        webhookEventConfig.setBusinessEventCode(jbmClusterBusinessEventBean.getEventCode());
        webhookEventConfig.setUrl(jbmClusterBusinessEventBean.getUrl());
        webhookEventConfig.setServiceName(jbmClusterBusinessEventBean.getServiceName());
        webhookEventConfig.setEventGroup(jbmClusterBusinessEventBean.getEventGroup());
        webhookEventConfig.setMethodType(jbmClusterBusinessEventBean.getMethodType());
        if (ObjectUtil.isEmpty(webhookEventConfig.getEnable())) {
            webhookEventConfig.setEnable(true);
        }
        if (ObjectUtil.isEmpty(webhookEventConfig.getGlobal())) {
            webhookEventConfig.setGlobal(false);
        }
        WebhookTask webhookTask = new WebhookTask();
        webhookTask.setEventId(webhookEventConfig.getEventId());
        webhookTask.setRequest(jbmClusterBusinessEventBean.getEventBody());
        webhookTaskForm.setWebhookTask(webhookTask);
        webhookTaskForm.setWebhookEventConfig(webhookEventConfig);
        return webhookTaskForm;
    }


}
