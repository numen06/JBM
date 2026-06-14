package com.jbm.cluster.push.service.impl;

import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
import com.jbm.cluster.core.constant.QueueConstants;
import com.jbm.cluster.push.model.PushRealtimeMessageEvent;
import com.jbm.cluster.push.service.PushRealtimeMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class PushRealtimeMessageServiceImpl implements PushRealtimeMessageService {

    @Autowired(required = false)
    private StreamBridge streamBridge;

    @Autowired(required = false)
    private SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void publish(PushMessageBody body, PushMessageItem item) {
        PushRealtimeMessageEvent event = buildEvent(body, item);
        if (streamBridge != null) {
            try {
                boolean sent = streamBridge.send(QueueConstants.PUSH_REALTIME_MESSAGE_STREAM, event);
                if (sent) {
                    return;
                }
                log.warn("站内信实时广播发送失败 msgId={}, recUserId={}", event.getMsgId(), event.getRecUserId());
            } catch (Exception e) {
                log.warn("站内信实时广播异常 msgId={}, recUserId={}", event.getMsgId(), event.getRecUserId(), e);
            }
        }
        deliver(event);
    }

    @Override
    public void deliver(PushRealtimeMessageEvent event) {
        if (event == null || event.getRecUserId() == null || simpMessagingTemplate == null) {
            return;
        }
        simpMessagingTemplate.convertAndSendToUser(String.valueOf(event.getRecUserId()), "/queue/messages", event);
    }

    private PushRealtimeMessageEvent buildEvent(PushMessageBody body, PushMessageItem item) {
        PushRealtimeMessageEvent event = new PushRealtimeMessageEvent();
        event.setMsgId(item.getMsgId());
        event.setMsgBodyId(item.getMsgBodyId());
        event.setRecUserId(item.getRecUserId());
        event.setSendUserId(item.getSendUserId());
        event.setSysMsg(item.getSendUserId() == null);
        event.setPushStatus(item.getPushStatus());
        event.setPushWay(item.getPushWay());
        event.setReadFlag(item.getReadFlag());
        event.setTitle(body.getTitle());
        event.setContent(body.getContent());
        event.setType(body.getType());
        event.setLevel(body.getLevel());
        event.setUrl(body.getUrl());
        event.setCreateTime(body.getCreateTime());
        event.setExtend(body.getExtend());
        Map<String, Object> extend = body.getExtend();
        if (extend != null) {
            Object testRunId = extend.get("testRunId");
            Object clientSentAt = extend.get("clientSentAt");
            event.setTestRunId(testRunId == null ? null : String.valueOf(testRunId));
            if (clientSentAt instanceof Number) {
                event.setClientSentAt(((Number) clientSentAt).longValue());
            } else if (clientSentAt != null) {
                try {
                    event.setClientSentAt(Long.valueOf(String.valueOf(clientSentAt)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return event;
    }
}
