package com.jbm.cluster.push.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class PushTestAck implements Serializable {

    private static final long serialVersionUID = 1L;

    private String testRunId;
    private String msgId;
    private Long recUserId;
    private Long receivedAt;
    private Long latencyMs;
}
