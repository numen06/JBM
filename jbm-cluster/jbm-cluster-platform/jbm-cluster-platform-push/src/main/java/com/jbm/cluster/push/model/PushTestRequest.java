package com.jbm.cluster.push.model;

import com.jbm.cluster.api.constants.push.PushMsgType;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class PushTestRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Long> recUserIds;
    private String tags;
    private String title;
    private String content;
    private PushMsgType pushMsgType;
    private Map<String, Object> extend;
    private Integer messageCount;
    private Integer batchSize;
    private Long intervalMillis;
    private Boolean waitAck;
    private Boolean showInMessageCenter;
}
