package com.jbm.cluster.push.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
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

import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

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


    /**
     * 删除两个月前的数据
     */
    @Override
    public boolean clearTasks() {
        QueryWrapper<WebhookTask> queryWrapper = currentQueryWrapper();
        queryWrapper.lambda().le(WebhookTask::getCreateTime, DateUtil.offsetMonth(DateTime.now(), -2));
        return this.deleteByWapper(queryWrapper);
    }


    @Override
    public void sendEvent(WebhookTaskForm webhookTaskForm) {
        List<WebhookEventConfig> webhookEventConfigList = CollUtil.newArrayList();
        //如果传输过来的数据中已经有配置则不再搜索
        if (ObjectUtil.isNotEmpty(webhookTaskForm.getWebhookEventConfig())) {
            //根据事件ID查询
            if (ObjectUtil.isNotEmpty(webhookTaskForm.getWebhookEventConfig().getEventId())) {
                webhookEventConfigList = CollUtil.newArrayList(webhookTaskForm.getWebhookEventConfig());
            } else {
                webhookEventConfigList = webhookEventConfigService.selectByEventCode(webhookTaskForm.getWebhookEventConfig().getBusinessEventCode());
            }
        }
        //系统分组默认发送全部
//        List<WebhookEventConfig> defGroup = webhookEventConfigList.stream().filter(item -> StrUtil.isEmpty(item.getEventGroup()) || BusinessEventConstant.SYSTEM.equals(item.getEventGroup())).collect(Collectors.toList());
//        defGroup.forEach(new Consumer<WebhookEventConfig>() {
//            @Override
//            public void accept(WebhookEventConfig webhookEventConfig) {
//                if (BooleanUtil.isFalse(webhookEventConfig.getEnable())) {
//                    return;
//                }
//                //如果是全局事件采用唯一性推送
//                if (BooleanUtil.isTrue(webhookEventConfig.getGlobal())) {
//                    sendEvent(webhookEventConfig, webhookTaskForm.getWebhookTask());
//                } else {
//                    sendEventAsync(webhookEventConfig, webhookTaskForm.getWebhookTask());
//                }
//            }
//        });
        //过滤掉不启用的配置
        webhookEventConfigList = webhookEventConfigList.stream().filter(item -> BooleanUtil.isTrue(item.getEnable())).collect(Collectors.toList());
        if (CollUtil.isEmpty(webhookEventConfigList)) {
            throw new ServiceException("不存在可用的发送配置");
        }
        Map<String, List<WebhookEventConfig>> groupEvnetGroup = webhookEventConfigList.stream().filter(item -> StrUtil.isNotBlank(item.getEventGroup())).collect(Collectors.groupingBy(WebhookEventConfig::getEventGroup));
        //遍历整个分组
        groupEvnetGroup.forEach(new BiConsumer<String, List<WebhookEventConfig>>() {
            @Override
            public void accept(String group, List<WebhookEventConfig> webhookEventConfigs) {

                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        //分拨推送
                        for (WebhookEventConfig webhookEventConfig : webhookEventConfigs) {
                            WebhookTask webhookTask = sendEvent(webhookEventConfig, webhookTaskForm.getWebhookTask());
                            //如果状态为200则为成功,跳出循环
                            if (webhookTask.getHttpStatus() == HttpStatus.HTTP_OK) {
                                log.info("分组名称:{},执行地址:{}", group, webhookTask.getEventId());
                                return;
                            }
//                            if (webhookTask.getRetryNumber() >= 3) {
//                                return;
//                            }
                        }
                    }
                });

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
        WebhookTaskForm webhookTaskForm = new WebhookTaskForm();
        webhookTaskForm.setWebhookTask(webhookTask);
        webhookTaskForm.setWebhookEventConfig(webhookEventConfig);
        this.sendEvent(webhookTaskForm);
//        this.sendEventAsync(webhookEventConfig, webhookTask);
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

    private void buildErrorMsg(WebhookTask webhookTask, String... errorMsg) {
        String format = "{} : {}";
        StringBuilder sb = new StringBuilder();
        sb.append(webhookTask.getErrorMsg());
        for (String s : errorMsg) {
            String msg = StrUtil.format(format, DateUtil.now(), StrUtil.emptyToDefault(s, "无"));
            sb.append("\r\n").append(msg);
        }
        webhookTask.setErrorMsg(sb.toString());
    }

    private WebhookTask sendEvent(WebhookEventConfig webhookEventConfig, WebhookTask sourceWebhookTask) {
        AtomicBoolean ok = new AtomicBoolean(true);
        WebhookTask webhookTask = ObjectUtil.isEmpty(sourceWebhookTask) || !StrUtil.equalsIgnoreCase(webhookEventConfig.getEventId(), sourceWebhookTask.getEventId()) ? new WebhookTask() : sourceWebhookTask;
        webhookTask.setRequest(sourceWebhookTask.getRequest());
        //初始化一个方法
        webhookTask.setEventId(webhookEventConfig.getEventId());
        if (ObjectUtil.isEmpty(webhookTask.getRetryNumber())) {
            webhookTask.setRetryNumber(0);
        }
        //如果事件则标注错误
        if (BooleanUtil.isFalse(webhookEventConfig.getEnable())) {
            //如果不启用则跳出
            this.buildErrorMsg(webhookTask, "事件未启用");
        }
        this.saveEntity(webhookTask);
        //如果事件未启用则跳出
        if (BooleanUtil.isFalse(webhookEventConfig.getEnable())) {
            return webhookTask;
        }
        while (ok.get() && !webhookTaskCache.containsKey(webhookTask.getTaskId())) {
            try {
                webhookTaskCache.put(webhookTask.getTaskId(), webhookTask);
                HttpResponse response = jbmRequestTemplate.request(webhookEventConfig.getUrl(), webhookEventConfig.getMethodType(), webhookTask.getRequest());
                webhookTask.setResponse(response.body());
                webhookTask.setHttpStatus(response.getStatus());
                this.buildErrorMsg(webhookTask);
                if (response.getStatus() != HttpStatus.HTTP_OK) {
                    throw new RuntimeException("推送HTTP状态码错误:" + response.getStatus());
                }
//                this.saveEntity(webhookTask);
                ok.set(false);
            } catch (UnknownHostException e) {
                webhookTask.setHttpStatus(HttpStatus.HTTP_NOT_FOUND);
                //如果超出重试次数跳出
                if (eventException(webhookTask, e)) {
                    break;
                }
            } catch (Exception e) {
                //如果超出重试次数跳出
                if (eventException(webhookTask, e)) {
                    break;
                }
            } finally {
                if (ObjectUtil.isEmpty(webhookTask.getHttpStatus())) {
                    webhookTask.setHttpStatus(HttpStatus.HTTP_NOT_FOUND);
                }
                //如果访问是404取消自动发送
                if (webhookTask.getHttpStatus().equals(HttpStatus.HTTP_NOT_FOUND)) {
                    webhookEventConfigService.disableEvents(webhookEventConfig.getServiceName());
                }
                //保存信息
                this.saveEntity(webhookTask);
                webhookTaskCache.remove(webhookTask.getTaskId());
//                BeanUtil.copyProperties(webhookEventConfig,sourceWebhookTask);
            }
        }
        return webhookTask;
    }

    private boolean eventException(WebhookTask webhookTask, Exception e) {
        webhookTask.setRetryNumber(webhookTask.getRetryNumber() + 1);
        log.error("执行远程业务事件错误", e);
        this.buildErrorMsg(webhookTask, e.getMessage());
//        webhookTask.setErrorMsg(e.getMessage());
        return webhookTask.getRetryNumber() >= 3;
    }
}