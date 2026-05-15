package com.jbm.cluster.push.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.message.WebhookEventConfig;
import com.jbm.cluster.api.entitys.message.WebhookTask;
import com.jbm.cluster.push.bevent.TaskStatus;
import com.jbm.cluster.push.bevent.WebhookEventService;
import com.jbm.cluster.push.bevent.lis.WebhookTaskEndEvent;
import com.jbm.cluster.push.form.WebhookTaskForm;
import com.jbm.cluster.push.mapper.WebhookTaskMapper;
import com.jbm.cluster.push.result.WebhookTaskResult;
import com.jbm.cluster.push.service.WebhookConfigSelectionStrategy;
import com.jbm.cluster.push.service.WebhookEventConfigService;
import com.jbm.cluster.push.service.WebhookTaskService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import jbm.framework.spring.config.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-08-30 16:36:49
 */
@Service
@Slf4j
public class WebhookTaskServiceImpl extends MasterDataServiceImpl<WebhookTask> implements WebhookTaskService {

    @Autowired
    private WebhookEventConfigService webhookEventConfigService;
    @Autowired
    private WebhookTaskMapper webhookTaskMapper;

    @Autowired
    private WebhookEventService webhookEventService;
    
    @Autowired
    private WebhookConfigSelectionStrategy configSelectionStrategy;

//    @Autowired
//    private WebhookTaskService webhookTaskService;

    /**
     * 删除两个月前的数据
     */
    @Override
    @Scheduled(cron = "0 0 1 1/1 * ?")
    public boolean clearTasks() {
        QueryWrapper<WebhookTask> queryWrapper = currentQueryWrapper();
        queryWrapper.lambda().le(WebhookTask::getCreateTime, DateUtil.offsetMonth(DateTime.now(), -2));
        return this.deleteByWapper(queryWrapper);
    }

    /**
     * 获取可用事件配置
     * @param webhookTaskForm
     * @return  List
     */
    @Override
    public List<WebhookEventConfig> getEnableEventConfigs(WebhookTaskForm webhookTaskForm) {
        List<WebhookEventConfig> webhookEventConfigList = CollUtil.newArrayList();
        //如果传输过来的数据中已经有配置则不再搜索
        if (ObjectUtil.isNotEmpty(webhookTaskForm.getWebhookEventConfig())) {
            //根据事件ID查询
            if (ObjectUtil.isNotEmpty(webhookTaskForm.getWebhookEventConfig().getEventId())) {
                webhookTaskForm.getWebhookEventConfig().setEnable(true);
                webhookEventConfigList = CollUtil.newArrayList(webhookTaskForm.getWebhookEventConfig());
            } else {
                webhookEventConfigList = webhookEventConfigService.selectByEventCode(webhookTaskForm.getWebhookEventConfig().getBusinessEventCode());
            }
        }
        //过滤掉不启用的配置
        webhookEventConfigList = webhookEventConfigList.stream().filter(item -> BooleanUtil.isTrue(item.getEnable())).collect(Collectors.toList());
        if (CollUtil.isEmpty(webhookEventConfigList)) {
            throw new ServiceException("不存在可用的发送配置");
        }
        return webhookEventConfigList;
    }



    @Override
    public void sendBusinessEvent(WebhookTaskForm webhookTaskForm) {
        List<WebhookEventConfig> webhookEventConfigList = getEnableEventConfigs(webhookTaskForm);
        Map<String, List<WebhookEventConfig>> groupEventGroup = webhookEventConfigList.stream().filter(item -> StrUtil.isNotBlank(item.getEventGroup())).collect(Collectors.groupingBy(WebhookEventConfig::getEventGroup));
        groupEventGroup.forEach((group, webhookEventConfigs)->{
            // 使用策略类选择最优配置
            WebhookEventConfig selectedConfig = configSelectionStrategy.selectConfig(group, webhookEventConfigs);
            if (selectedConfig == null) {
                log.warn("分组 {} 中没有可用的配置", group);
                return;
            }
            
            // 尝试发送事件
            try {
                this.sendBusinessEvent(selectedConfig, webhookTaskForm.getWebhookTask());
                // 发送成功，更新策略
                configSelectionStrategy.handleSuccess(group, selectedConfig);
            } catch (Exception e) {
                log.error("使用配置 {} 发送事件失败", selectedConfig.getEventId(), e);
                // 发送失败，更新策略
                configSelectionStrategy.handleFailure(group, selectedConfig);
                
                // 遍历分组中的其他配置尝试发送
                for (WebhookEventConfig config : webhookEventConfigs) {
                    // 跳过已尝试过的配置
                    if (StrUtil.equals(config.getEventId(), selectedConfig.getEventId())) {
                        continue;
                    }
                    
                    try {
                        this.sendBusinessEvent(config, webhookTaskForm.getWebhookTask());
                        // 发送成功，更新策略并退出循环
                        configSelectionStrategy.handleSuccess(group, config);
                        break;
                    } catch (Exception ex) {
                        log.error("使用配置 {} 发送事件失败", config.getEventId(), ex);
                        configSelectionStrategy.handleFailure(group, config);
                    }
                }
            }
        });
    }

    @Override
    public WebhookTask selectByTaskId(String taskId) {
        QueryWrapper<WebhookTask> queryWrapper = currentQueryWrapper();
        queryWrapper.lambda().eq(WebhookTask::getTaskId, taskId);
        return this.selectEntityByWapper(queryWrapper);
    }

    @Override
    public void sendBusinessEvent(String eventId) {
        WebhookEventConfig webhookEventConfig = webhookEventConfigService.selectByEventId(eventId);
        if (ObjectUtil.isEmpty(webhookEventConfig)) {
            throw new ServiceException("事件为空");
        }
        WebhookTask webhookTask = new WebhookTask();
        this.sendBusinessEvent(webhookEventConfig, webhookTask);
    }

    @Override
    public void sendBusinessEvent(WebhookTask webhookTask) {
        WebhookEventConfig webhookEventConfig = webhookEventConfigService.selectByEventId(webhookTask.getEventId());
        WebhookTaskForm webhookTaskForm = new WebhookTaskForm();
        webhookTaskForm.setWebhookTask(webhookTask);
        webhookTaskForm.setWebhookEventConfig(webhookEventConfig);
        this.sendBusinessEvent(webhookTaskForm);
//        this.sendEventAsync(webhookEventConfig, webhookTask);
    }

    @Override
    public void retryEventTask(String taskId) {
        WebhookTask webhookTask = this.selectByTaskId(taskId);
        if (ObjectUtil.isEmpty(webhookTask)) {
            throw new ServiceException("任务不存在");
        }
        this.sendBusinessEvent(webhookTask);
    }

    @Override
    public DataPaging<WebhookTaskResult> selectWebhookTasks(WebhookTaskForm webhookTaskForm) {
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

//    private final Map<String, WebhookTask> webhookTaskCache = new ConcurrentHashMap<>();


//    private void sendEventAsync(WebhookEventConfig webhookEventConfig, WebhookTask sourceWebhookTask) {
//        Future<?> future = executorService.submit(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    log.info("推送任务开始:{}", webhookEventConfig.getEventName());
//                    sendBusinessEvent(webhookEventConfig, sourceWebhookTask);
//                } catch (Exception e) {
//                    log.error("推送Webhook事件错误", e);
//                } finally {
//                    log.info("推送任务完成:{}", webhookEventConfig.getEventName());
//                }
//            }
//        });
//    }

    private void buildErrorMsg(WebhookTask webhookTask, String... errorMsg) {
        String format = "{} : {}";
        StringBuilder sb = new StringBuilder();
        sb.append(StrUtil.emptyIfNull(webhookTask.getErrorMsg()));
        for (String s : errorMsg) {
            String msg = StrUtil.format(format, DateUtil.now(), StrUtil.emptyToDefault(s, "无"));
            sb.append("\r\n").append(msg);
        }
        webhookTask.setErrorMsg(StrUtil.trimToEmpty(sb.toString()));
    }



    /**
     * 组装任务或者中数据中找到这个任务
     * @param webhookEventConfig
     * @param sourceWebhookTask
     * @return
     */
    private WebhookTask sendBusinessEvent(WebhookEventConfig webhookEventConfig, WebhookTask sourceWebhookTask) {
        // 生成唯一事件ID
        WebhookTask webhookTask = ObjectUtil.isEmpty(sourceWebhookTask) || !StrUtil.equalsIgnoreCase(webhookEventConfig.getEventId(), sourceWebhookTask.getEventId()) ? new WebhookTask() : sourceWebhookTask;
        webhookTask.setRequest(sourceWebhookTask.getRequest());
        webhookTask.setTaskUrl(webhookEventConfig.getUrl());
        webhookTask.setTaskMethod(webhookEventConfig.getMethodType());
        if (ObjectUtil.isEmpty(webhookTask.getRequest())) {
            webhookTask.setRequest(webhookEventConfig.getEventBody());
        }
        //初始化一个方法
        webhookTask.setEventId(webhookEventConfig.getEventId());
        if (ObjectUtil.isEmpty(webhookTask.getRetryNumber())) {
            webhookTask.setRetryNumber(0);
        }
        WebhookTaskService webhookTaskService = SpringContextHolder.getBean(WebhookTaskService.class);
        //如果事件则标注错误
        if (BooleanUtil.isFalse(webhookEventConfig.getEnable())) {
            //如果不启用则跳出
            this.buildErrorMsg(webhookTask, "事件未启用");
            webhookTaskService.saveEntity(webhookTask);
            return webhookTask;
        }
        //创建任务
        webhookTaskService.saveEntity(webhookTask);
        //投递事件
        webhookEventService.processEvent(webhookTask);
        return webhookTask;
    }

    /**
     *
     * 事件结束
     * @param webhookTaskEndEvent
     */
    @Async
    @EventListener
    public void webhookEventEnd(WebhookTaskEndEvent webhookTaskEndEvent) {
        WebhookTaskService webhookTaskService = SpringContextHolder.getBean(WebhookTaskService.class);
        try {
            WebhookTask task = webhookTaskEndEvent.getWebhookTask();
            // 确保状态和重试次数都被更新
            if (webhookTaskEndEvent.getTaskStatus() == TaskStatus.SUCCESS) {
                task.setStatus(TaskStatus.SUCCESS.toString());
            } else {
                task.setStatus(TaskStatus.FAILED.toString());
            }
            // 重要：同时更新 retryNumber，确保重试次数被保存到数据库
            // task 对象中的 retryNumber 已经在 sendTaskWithRetry 中更新过了
            webhookTaskService.updateEntity(task);
            log.debug("✅ 任务 {} 状态已更新: status={}, retryNumber={}", 
                    task.getTaskId(), task.getStatus(), task.getRetryNumber());
        }catch (Exception e){
            log.error("更新任务错误", e);
        }
    }

    private boolean eventException(WebhookTask webhookTask, Exception e) {
        webhookTask.setRetryNumber(webhookTask.getRetryNumber() + 1);
        log.error("执行远程业务事件错误", e);
        this.buildErrorMsg(webhookTask, e.getMessage());
//        webhookTask.setErrorMsg(e.getMessage());
        return webhookTask.getRetryNumber() >= 3;
    }
}