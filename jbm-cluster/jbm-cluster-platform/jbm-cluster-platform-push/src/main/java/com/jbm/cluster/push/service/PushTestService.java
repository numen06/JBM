package com.jbm.cluster.push.service;

import com.jbm.cluster.push.model.PushTestAck;
import com.jbm.cluster.push.model.PushTestRequest;
import com.jbm.cluster.push.model.PushTestTaskStatus;

public interface PushTestService {

    PushTestTaskStatus send(PushTestRequest request);

    PushTestTaskStatus startPerf(PushTestRequest request);

    PushTestTaskStatus getStatus(String taskId);

    PushTestTaskStatus ack(PushTestAck ack);
}
