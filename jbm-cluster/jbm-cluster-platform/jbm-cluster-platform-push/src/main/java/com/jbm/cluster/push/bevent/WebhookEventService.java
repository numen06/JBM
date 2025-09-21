package com.jbm.cluster.push.bevent;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.common.basic.module.JbmRequestTemplate;
import com.jbm.cluster.push.bevent.lis.WebhookTaskEndEvent;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * @author wesley
 */
@Service
@Slf4j
public class WebhookEventService {


    @Autowired
    private EventDeliveryService eventDeliveryService;

    private JbmRequestTemplate jbmRequestTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 🎯 统一入口：支持“失败后 sticky 队列”，保证全局顺序
     */
    public void processEvent(WebhookTask task) {
        if (StrUtil.isEmpty( task.getTaskUrl())) {
            throw new IllegalArgumentException("taskUrl is empty");
        }
        if (StrUtil.isEmpty(task.getTaskId())) {
            task.setTaskId(IdUtil.fastSimpleUUID());
        }
        if (task.getCreateTime() == null) {
            task.setCreateTime(DateTime.now());
        }

        final String url = task.getTaskUrl();

        // 🚀 首次尝试直发
        try {
            Response response = jbmRequestTemplate.request(url, task.getTaskMethod(), task.getRequest());
            if (response.isSuccessful()) {
                // ✅ 成功 → 直接返回
                task.setStatus(TaskStatus.SUCCESS.toString());
                task.setHttpStatus(response.code());
                log.debug("✅ 直发成功: {}", url);
                eventPublisher.publishEvent(WebhookTaskEndEvent.success(this, task));
            } else {
                throw new RuntimeException("HTTP " + response.code());
            }
            //模拟发送失败
//            throw new RuntimeException("模拟发送失败");
        } catch (Exception e) {
            log.warn("⚠️ 直发失败，标记为异步发送并入队: {}", url, e);
            eventDeliveryService.enqueueAndTrigger(task);
        }
    }

    /**
     * 🆕 封装：入队列 + 触发投递
     */

}