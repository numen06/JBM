package com.jbm.cluster.push.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.message.WebhookEventConfig;
import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.common.basic.module.JbmRequestTemplate;
import com.jbm.cluster.push.form.WebhookTaskForm;
import com.jbm.cluster.push.mapper.WebhookTaskMapper;
import com.jbm.cluster.push.result.WebhookTaskReslut;
import com.jbm.cluster.push.service.WebhookEventConfigService;
import com.jbm.cluster.push.service.WebhookTaskService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MultiPlatformServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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
    @Autowired
    private WebhookTaskMapper webhookTaskMapper;


    /***
     * 异步执行线程池
     */
    private ExecutorService executorService = ThreadUtil.newExecutor(100);

    @Override
    public void sendEvent(WebhookTaskForm webhookTaskForm) {
        List<WebhookEventConfig> webhookEventConfigs = webhookEventConfigService.selectByEventCode(webhookTaskForm.getWebhookEventConfig().getBusinessEventCode());
        if (CollUtil.isEmpty(webhookEventConfigs)) {
            return;
        }
        webhookEventConfigs.forEach(webhookEventConfig -> {
            sendEventAsync(webhookEventConfig, webhookTaskForm.getWebhookTask());
        });
    }

    @Override
    public WebhookTask selectByTaskId(String taskId) {
        QueryWrapper<WebhookTask> queryWrapper = currentQueryWrapper();
        queryWrapper.lambda().eq(WebhookTask::getTaskId, taskId);
        return this.selectEntityByWapper(queryWrapper);
    }

    @Override
    public void sendEvent(String eventId) {
        WebhookEventConfig webhookEventConfig = webhookEventConfigService.selectByEventId(eventId);
        if (ObjectUtil.isEmpty(webhookEventConfig)) {
            throw new ServiceException("事件为空");
        }
        WebhookTask webhookTask = new WebhookTask();
        this.sendEvent(webhookEventConfig, webhookTask);
    }

    @Override
    public void sendEvent(WebhookTask webhookTask) {
        WebhookEventConfig webhookEventConfig = webhookEventConfigService.selectByEventId(webhookTask.getEventId());
        this.sendEventAsync(webhookEventConfig, webhookTask);
    }

    @Override
    public void retryEventTask(String taskId) {
        WebhookTask webhookTask = this.selectByTaskId(taskId);
        if (ObjectUtil.isEmpty(webhookTask)) {
            throw new ServiceException("任务不存在");
        }
        this.sendEvent(webhookTask);
    }

    @Override
    public DataPaging<WebhookTaskReslut> selectWebhookTasks(WebhookTaskForm webhookTaskForm) {
        return this.selectPageList(webhookTaskForm.getPageForm(), (page) -> {
            if (ObjectUtil.isEmpty(webhookTaskForm.getWebhookTask())) {
                webhookTaskForm.setWebhookTask(new WebhookTask());
            }
            if (ObjectUtil.isEmpty(webhookTaskForm.getWebhookEventConfig())) {
                webhookTaskForm.setWebhookEventConfig(new WebhookEventConfig());
            }
            webhookTaskMapper.selectWebhookTasks(page, webhookTaskForm);
        });
    }

    private Map<String, WebhookTask> webhookTaskCache = new ConcurrentHashMap<>();



    private void sendEventAsync(WebhookEventConfig webhookEventConfig, WebhookTask sourceWebhookTask) {
        Future<?> future = executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    log.info("推送任务开始:{}", webhookEventConfig.getEventName());
                    sendEvent(webhookEventConfig, sourceWebhookTask);
                } catch (Exception e) {
                    log.error("推送Webhook事件错误", e);
                } finally {
                    log.info("推送任务完成:{}", webhookEventConfig.getEventName());
                }
            }
        });
    }

    private void sendEvent(WebhookEventConfig webhookEventConfig, WebhookTask sourceWebhookTask) {
        AtomicBoolean ok = new AtomicBoolean(true);
        WebhookTask webhookTask = ObjectUtil.isEmpty(sourceWebhookTask) || !StrUtil.equalsIgnoreCase(webhookEventConfig.getEventId(), sourceWebhookTask.getEventId()) ? new WebhookTask() : sourceWebhookTask;
        webhookTask.setRequest(sourceWebhookTask.getRequest());
        //初始化一个方法
        webhookTask.setEventId(webhookEventConfig.getEventId());
        if (ObjectUtil.isEmpty(webhookTask.getRetryNumber())) {
            webhookTask.setRetryNumber(0);
        }
        this.saveEntity(webhookTask);
        while (ok.get() && !webhookTaskCache.containsKey(webhookTask.getTaskId())) {
            try {
                webhookTaskCache.put(webhookTask.getTaskId(), webhookTask);
                HttpResponse response = jbmRequestTemplate.request(webhookEventConfig.getUrl(), webhookEventConfig.getMethodType(), webhookTask.getRequest());
                webhookTask.setResponse(response.body());
                webhookTask.setHttpStatus(response.getStatus());
                webhookTask.setErrorMsg("无");
//                this.saveEntity(webhookTask);
                ok.set(false);
            } catch (Exception e) {
                ThreadUtil.safeSleep(500);
                webhookTask.setRetryNumber(webhookTask.getRetryNumber() + 1);
                log.error("执行远程业务事件错误", e);
                webhookTask.setErrorMsg(e.getMessage());
                if (webhookTask.getRetryNumber() >= 3) {
                    break;
                }
            } finally {
                if (ObjectUtil.isEmpty(webhookTask.getHttpStatus())) {
                    webhookTask.setHttpStatus(404);
                }
                //保存信息
                this.saveEntity(webhookTask);
                webhookTaskCache.remove(webhookTask.getTaskId());
            }
        }
    }
}