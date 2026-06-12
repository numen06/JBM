package com.jbm.cluster.push.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.constants.push.PushMsgType;
import com.jbm.cluster.api.constants.push.PushWay;
import com.jbm.cluster.api.model.push.PushMsg;
import com.jbm.cluster.common.basic.module.JbmClusterNotification;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.push.model.PushTestAck;
import com.jbm.cluster.push.model.PushTestRequest;
import com.jbm.cluster.push.model.PushTestTaskStatus;
import com.jbm.cluster.push.service.PushRecipientResolver;
import com.jbm.cluster.push.service.PushTestService;
import com.jbm.framework.exceptions.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class PushTestServiceImpl implements PushTestService, DisposableBean {

    private static final int MAX_LIGHT_MESSAGES = 5000;

    @Autowired
    private JbmClusterNotification jbmClusterNotification;
    @Autowired
    private PushRecipientResolver pushRecipientResolver;

    private final Map<String, PushTestTaskStatus> tasks = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> latencyTotals = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @Override
    public PushTestTaskStatus send(PushTestRequest request) {
        PushTestRequest safeRequest = normalizeRequest(request);
        PushTestTaskStatus task = createTask(safeRequest, 1);
        sendOne(safeRequest, task.getTaskId(), 1);
        task.setSentCount(1L);
        task.setStatus("SENT");
        task.setFinishedAt(System.currentTimeMillis());
        return task;
    }

    @Override
    public PushTestTaskStatus startPerf(PushTestRequest request) {
        PushTestRequest safeRequest = normalizeRequest(request);
        int messageCount = normalizeMessageCount(safeRequest.getMessageCount());
        PushTestTaskStatus task = createTask(safeRequest, messageCount);
        task.setStatus("RUNNING");
        executor.submit(() -> runPerf(safeRequest, task, messageCount));
        return task;
    }

    @Override
    public PushTestTaskStatus getStatus(String taskId) {
        return tasks.get(taskId);
    }

    @Override
    public PushTestTaskStatus ack(PushTestAck ack) {
        if (ack == null || StrUtil.isBlank(ack.getTestRunId())) {
            return null;
        }
        PushTestTaskStatus task = tasks.get(ack.getTestRunId());
        if (task == null) {
            return null;
        }
        long latency = ack.getLatencyMs() != null ? ack.getLatencyMs() : 0L;
        if (latency <= 0 && ack.getReceivedAt() != null) {
            latency = Math.max(0L, System.currentTimeMillis() - ack.getReceivedAt());
        }
        task.setAckCount(ObjectUtil.defaultIfNull(task.getAckCount(), 0L) + 1L);
        if (latency > 0) {
            latencyTotals.computeIfAbsent(task.getTaskId(), key -> new AtomicLong()).addAndGet(latency);
            task.setMaxLatencyMs(Math.max(ObjectUtil.defaultIfNull(task.getMaxLatencyMs(), 0L), latency));
            task.setAvgLatencyMs(latencyTotals.get(task.getTaskId()).get() / task.getAckCount());
        }
        return task;
    }

    private void runPerf(PushTestRequest request, PushTestTaskStatus task, int messageCount) {
        long interval = request != null && request.getIntervalMillis() != null ? Math.max(0L, request.getIntervalMillis()) : 0L;
        int batchSize = request != null && request.getBatchSize() != null && request.getBatchSize() > 0 ? request.getBatchSize() : 1;
        try {
            for (int i = 1; i <= messageCount; i++) {
                sendOne(request, task.getTaskId(), i);
                task.setSentCount(ObjectUtil.defaultIfNull(task.getSentCount(), 0L) + 1L);
                if (interval > 0L && i < messageCount && i % batchSize == 0) {
                    Thread.sleep(interval);
                }
            }
            task.setStatus("FINISHED");
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setFailedCount(ObjectUtil.defaultIfNull(task.getFailedCount(), 0L) + 1L);
            log.error("Push轻压测任务失败 taskId={}", task.getTaskId(), e);
        } finally {
            task.setFinishedAt(System.currentTimeMillis());
        }
    }

    private PushTestTaskStatus createTask(PushTestRequest request, int messageCount) {
        PushMsg pushMsg = buildPushMsg(request, UUID.randomUUID().toString(), 0);
        Set<Long> users = pushRecipientResolver.resolve(pushMsg);
        if (CollUtil.isEmpty(users)) {
            throw new ServiceException("测试消息未解析到接收用户");
        }
        PushTestTaskStatus task = new PushTestTaskStatus();
        task.setTaskId((String) pushMsg.getExtend().get("testRunId"));
        task.setStatus("CREATED");
        task.setRequestedMessages(messageCount);
        task.setResolvedUsers(users.size());
        task.setStartedAt(System.currentTimeMillis());
        task.setSentCount(0L);
        task.setFailedCount(0L);
        task.setAckCount(0L);
        task.setAvgLatencyMs(0L);
        task.setMaxLatencyMs(0L);
        tasks.put(task.getTaskId(), task);
        latencyTotals.put(task.getTaskId(), new AtomicLong());
        return task;
    }

    private void sendOne(PushTestRequest request, String taskId, int sequence) {
        PushMsg pushMsg = buildPushMsg(request, taskId, sequence);
        jbmClusterNotification.pushMsg(pushMsg);
    }

    private PushMsg buildPushMsg(PushTestRequest request, String taskId, int sequence) {
        PushTestRequest safeRequest = request == null ? new PushTestRequest() : request;
        PushMsg pushMsg = new PushMsg();
        pushMsg.setSysMsg(true);
        pushMsg.setSendUserId(LoginHelper.softGetLoginUser() == null ? null : LoginHelper.softGetLoginUser().getUserId());
        pushMsg.setRecUserIds(CollUtil.isEmpty(safeRequest.getRecUserIds()) ? defaultCurrentUser() : safeRequest.getRecUserIds());
        pushMsg.setTags(safeRequest.getTags());
        pushMsg.setPushWays(Lists.newArrayList(PushWay.internal));
        pushMsg.setPushMsgType(ObjectUtil.defaultIfNull(safeRequest.getPushMsgType(), PushMsgType.notification));
        pushMsg.setTitle(StrUtil.blankToDefault(safeRequest.getTitle(), "Push通讯测试"));
        String content = StrUtil.blankToDefault(safeRequest.getContent(), "Push WebSocket闭环测试消息");
        pushMsg.setContent(sequence > 0 ? content + " #" + sequence : content);
        Map<String, Object> extend = new LinkedHashMap<>();
        if (safeRequest.getExtend() != null) {
            extend.putAll(safeRequest.getExtend());
        }
        extend.put("testRunId", taskId);
        extend.put("sequence", sequence);
        extend.put("clientSentAt", System.currentTimeMillis());
        pushMsg.setExtend(extend);
        return pushMsg;
    }

    private java.util.List<Long> defaultCurrentUser() {
        if (LoginHelper.softGetLoginUser() == null || LoginHelper.softGetLoginUser().getUserId() == null) {
            return null;
        }
        return Lists.newArrayList(LoginHelper.softGetLoginUser().getUserId());
    }

    private PushTestRequest normalizeRequest(PushTestRequest request) {
        PushTestRequest safeRequest = request == null ? new PushTestRequest() : request;
        if (CollUtil.isEmpty(safeRequest.getRecUserIds()) && StrUtil.isBlank(safeRequest.getTags())) {
            safeRequest.setRecUserIds(defaultCurrentUser());
        }
        return safeRequest;
    }

    private int normalizeMessageCount(Integer count) {
        if (count == null || count <= 0) {
            return 100;
        }
        return Math.min(count, MAX_LIGHT_MESSAGES);
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
    }
}
