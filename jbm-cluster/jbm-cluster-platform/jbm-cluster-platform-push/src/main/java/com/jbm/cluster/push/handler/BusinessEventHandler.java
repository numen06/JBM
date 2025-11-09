package com.jbm.cluster.push.handler;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.message.WebhookEventConfig;
import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.api.model.event.JbmClusterBusinessEventBean;
import com.jbm.cluster.api.model.event.JbmClusterBusinessEventResource;
import com.jbm.cluster.push.form.WebhookTaskForm;
import com.jbm.cluster.push.service.WebhookEventConfigService;
import com.jbm.cluster.push.service.WebhookTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Configuration
public class BusinessEventHandler {


    @Autowired
    private WebhookEventConfigService webhookEventConfigService;


    @Autowired
    private WebhookTaskService webhookTaskService;

    /**
     * 接受注册集群事件
     */
    @Bean
    public Consumer<Message<JbmClusterBusinessEventResource>> businessEventResource() {
        return message -> {
            // 调用你的业务逻辑
            receive(message.getPayload());
        };
    }

    @Bean
    public Consumer<Message<JbmClusterBusinessEventBean>> businessEvent() {
        return message -> {
            // 调用你的业务逻辑
            sendBusinessEvent(message.getPayload());
        };
    }


    /**
     * 接受集群事件推送
     *
     */
    public void sendBusinessEvent(JbmClusterBusinessEventBean jbmClusterBusinessEventBean) {
        try {
            log.info("📥 接收到业务事件推送: {}", jbmClusterBusinessEventBean.getEventCode());
            
            if (log.isDebugEnabled()) {
                log.debug("   ├─ 服务名: {}", jbmClusterBusinessEventBean.getServiceName());
                log.debug("   ├─ 事件名称: {}", jbmClusterBusinessEventBean.getEventName());
                log.debug("   ├─ 事件分组: {}", jbmClusterBusinessEventBean.getEventGroup());
                log.debug("   ├─ URL: {}", jbmClusterBusinessEventBean.getUrl());
                log.debug("   ├─ 启用状态: {}", jbmClusterBusinessEventBean.getEnable());
                log.debug("   ├─ 全局标识: {}", jbmClusterBusinessEventBean.getGlobal());
                log.debug("   ├─ 方法类型: {}", jbmClusterBusinessEventBean.getMethodType());
                log.debug("   └─ 事件内容: {}", jbmClusterBusinessEventBean.getEventBody());
            }
            
            // 验证必要字段
            if (StrUtil.isBlank(jbmClusterBusinessEventBean.getEventCode())) {
                log.error("❌ 事件代码为空，无法处理该事件");
                return;
            }
            
            WebhookTaskForm webhookTaskForm = beanToWebHook(jbmClusterBusinessEventBean);
            
            if (log.isDebugEnabled()) {
                log.debug("🔄 转换为 WebhookTaskForm");
                log.debug("   ├─ BusinessEventCode: {}", 
                        webhookTaskForm.getWebhookEventConfig().getBusinessEventCode());
                log.debug("   ├─ URL: {}", 
                        webhookTaskForm.getWebhookEventConfig().getUrl());
                log.debug("   ├─ EventId: {}", 
                        webhookTaskForm.getWebhookEventConfig().getEventId());
                log.debug("   └─ Enable: {}", 
                        webhookTaskForm.getWebhookEventConfig().getEnable());
            }
            
            webhookTaskService.sendBusinessEvent(webhookTaskForm);
            
            log.debug("✅ 业务事件处理完成: {}", jbmClusterBusinessEventBean.getEventCode());
        } catch (Exception e) {
            log.error("❌ 接受集群事件推送失败: {}, 服务名: {}", 
                    jbmClusterBusinessEventBean.getEventCode(),
                    jbmClusterBusinessEventBean.getServiceName(), e);
        }
    }

    /**
     * 接受应用程序推过来的事件,进行注册
     *
     */
    public void receive(JbmClusterBusinessEventResource jbmClusterBusinessEventResource) {
        List<JbmClusterBusinessEventBean> jbmClusterBusinessEventBeans = jbmClusterBusinessEventResource.getJbmClusterBusinessEventBeans();
        final String batchTime = DateUtil.now();
        
        log.info("📝 接收到事件注册请求，服务ID: {}, 事件数量: {}, 批次时间: {}", 
                jbmClusterBusinessEventResource.getServiceId(), 
                jbmClusterBusinessEventBeans.size(),
                batchTime);
        
        jbmClusterBusinessEventBeans.forEach(jbmClusterBusinessEventBean -> {
            log.debug("   🔧 注册事件: {}, URL: {}", 
                    jbmClusterBusinessEventBean.getEventCode(),
                    jbmClusterBusinessEventBean.getUrl());
            
            WebhookTaskForm webhookTaskForm = beanToWebHook(jbmClusterBusinessEventBean);
            WebhookEventConfig webhookEventConfig = webhookEventConfigService.selectByCodeUrl(
                    webhookTaskForm.getWebhookEventConfig().getBusinessEventCode(), 
                    webhookTaskForm.getWebhookEventConfig().getUrl());
            
            if (ObjectUtil.isNotEmpty(webhookEventConfig)) {
                log.debug("      └─ 找到现有配置，EventId: {}, 将更新", webhookEventConfig.getEventId());
                webhookTaskForm.getWebhookEventConfig().setEventId(webhookEventConfig.getEventId());
            } else {
                log.debug("      └─ 未找到现有配置，将创建新配置");
            }
            
            webhookTaskForm.getWebhookEventConfig().setBatchTime(batchTime);
            webhookEventConfigService.saveEntity(webhookTaskForm.getWebhookEventConfig());
            log.debug("      ✅ 配置保存成功");
        });
        
        log.info("🧹 清理旧批次配置，服务ID: {}, 保留批次: {}", 
                jbmClusterBusinessEventResource.getServiceId(), batchTime);
        webhookEventConfigService.deleteOldBatch(jbmClusterBusinessEventResource.getServiceId(), batchTime);
        
        log.info("✅ 事件注册完成，服务ID: {}", jbmClusterBusinessEventResource.getServiceId());
    }

    /***
     * 将扫描数据转为成传输数据
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
