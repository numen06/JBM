package com.jbm.cluster.push.service;

import com.jbm.cluster.api.entitys.message.PushMessageBody;
import com.jbm.cluster.api.entitys.message.PushMessageItem;
import com.jbm.cluster.push.model.PushRealtimeMessageEvent;

public interface PushRealtimeMessageService {

    void publish(PushMessageBody body, PushMessageItem item);

    void deliver(PushRealtimeMessageEvent event);
}
