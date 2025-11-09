package com.jbm.cluster.push.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
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
import com.jbm.framework.service.mybatis.MultiPlatformServiceImpl;
import com.jbm.framework.usage.paging.DataPaging;
import jbm.framework.spring.config.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-08-30 16:36:49
 */
@Service
@Slf4j
public class WebhookTaskServiceImpl extends MultiPlatformServiceImpl<WebhookTask> implements WebhookTaskService {

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

    /***
     * 异步执行线程池
     */
    private final ExecutorService executorService = ThreadUtil.newExecutor(100);


    /**
     * 删除两个月前的数据
     */
    @Override
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
        String businessEventCode = null;
        //如果传输过来的数据中已经有配置则不再搜索
        if (ObjectUtil.isNotEmpty(webhookTaskForm.getWebhookEventConfig())) {
            businessEventCode = webhookTaskForm.getWebhookEventConfig().getBusinessEventCode();
            log.debug("🔍 开始查询业务事件配置，事件代码: {}", businessEventCode);
            
            //根据事件ID查询
            if (ObjectUtil.isNotEmpty(webhookTaskForm.getWebhookEventConfig().getEventId())) {
                log.debug("✅ 使用传入的事件ID: {}", webhookTaskForm.getWebhookEventConfig().getEventId());
                webhookTaskForm.getWebhookEventConfig().setEnable(true);
                webhookEventConfigList = CollUtil.newArrayList(webhookTaskForm.getWebhookEventConfig());
            } else {
                log.debug("📡 从数据库查询事件配置，事件代码: {}", businessEventCode);
                List<WebhookEventConfig> allConfigs = webhookEventConfigService.selectByEventCode(businessEventCode);
                webhookEventConfigList = allConfigs;
                
                // 记录查询到的配置信息，方便排查
                if (CollUtil.isNotEmpty(allConfigs)) {
                    log.info("📋 业务事件 [{}] 查询到 {} 个配置", businessEventCode, allConfigs.size());
                    long enabledCount = allConfigs.stream().filter(item -> BooleanUtil.isTrue(item.getEnable())).count();
                    long disabledCount = allConfigs.stream().filter(item -> BooleanUtil.isFalse(item.getEnable())).count();
                    log.info("   └─ 启用: {} 个, 禁用: {} 个", enabledCount, disabledCount);
                    
                    // 详细输出每个配置的状态
                    allConfigs.forEach(config -> {
                        log.debug("   ├─ EventId: {}, Enable: {}, URL: {}, ServiceName: {}", 
                                config.getEventId(), 
                                config.getEnable(), 
                                config.getUrl(),
                                config.getServiceName());
                    });
                } else {
                    log.warn("⚠️  业务事件 [{}] 在数据库中未找到任何配置记录", businessEventCode);
                }
            }
        } else {
            log.warn("⚠️  WebhookTaskForm 中的 WebhookEventConfig 为空");
        }
        
        //过滤掉不启用的配置
        int beforeFilterCount = webhookEventConfigList.size();
        webhookEventConfigList = webhookEventConfigList.stream().filter(item -> BooleanUtil.isTrue(item.getEnable())).collect(Collectors.toList());
        int afterFilterCount = webhookEventConfigList.size();
        
        if (beforeFilterCount != afterFilterCount) {
            log.debug("🔧 过滤后: {} 个配置 → {} 个启用的配置", beforeFilterCount, afterFilterCount);
        }
        
        if (CollUtil.isEmpty(webhookEventConfigList)) {
            String warnMsg = StrUtil.isNotBlank(businessEventCode) 
                    ? String.format("不存在可用的发送配置，业务事件代码: %s", businessEventCode)
                    : "不存在可用的发送配置，请检查 WebhookEventConfig 配置表";
            log.warn("⚠️  {}", warnMsg);
            log.warn("💡 可能原因: 1) 配置尚未注册 2) 配置已禁用(enable=false) 3) 目标服务未上线");
            log.warn("📝 建议: 如需推送该事件，请在目标服务中添加 @BusinessEvent 注解并重启服务");
            // 返回空列表，不抛出异常，避免中断整个流程
            return CollUtil.newArrayList();
        }
        
        log.info("✅ 找到 {} 个可用的发送配置", webhookEventConfigList.size());
        return webhookEventConfigList;
    }



    @Override
    public void sendBusinessEvent(WebhookTaskForm webhookTaskForm) {
        List<WebhookEventConfig> webhookEventConfigList = getEnableEventConfigs(webhookTaskForm);
        
        // 如果没有可用的配置，记录日志后直接返回，不进行推送
        if (CollUtil.isEmpty(webhookEventConfigList)) {
            log.info("⏭️  跳过事件推送，因为没有可用的配置");
            return;
        }
        
        Map<String, List<WebhookEventConfig>> groupEventGroup = webhookEventConfigList.stream().filter(item -> StrUtil.isNotBlank(item.getEventGroup())).collect(Collectors.groupingBy(WebhookEventConfig::getEventGroup));
        
        if (groupEventGroup.isEmpty()) {
            log.warn("⚠️  所有配置都没有设置事件分组(eventGroup)，无法进行推送");
            return;
        }
        
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
            if (webhookTaskEndEvent.getTaskStatus() == TaskStatus.SUCCESS) {
                webhookTaskEndEvent.getWebhookTask().setStatus(TaskStatus.SUCCESS.toString());
            } else {
                webhookTaskEndEvent.getWebhookTask().setStatus(TaskStatus.FAILED.toString());
            }
            webhookTaskService.updateEntity(webhookTaskEndEvent.getWebhookTask());
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