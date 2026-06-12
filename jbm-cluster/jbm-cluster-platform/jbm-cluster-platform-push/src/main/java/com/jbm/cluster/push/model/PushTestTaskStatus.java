package com.jbm.cluster.push.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class PushTestTaskStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskId;
    private String status;
    private Integer requestedMessages;
    private Integer resolvedUsers;
    private Long startedAt;
    private Long finishedAt;
    private Long sentCount;
    private Long failedCount;
    private Long ackCount;
    private Long avgLatencyMs;
    private Long maxLatencyMs;
    private String errorMessage;
}
