package com.jbm.cluster.push.model;

import com.jbm.cluster.api.constants.push.PushMsgType;
import com.jbm.cluster.api.constants.push.PushStatus;
import com.jbm.cluster.api.constants.push.PushWay;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Data
public class PushRealtimeMessageEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String msgId;
    private Long msgBodyId;
    private Long recUserId;
    private Long sendUserId;
    private Boolean sysMsg;
    private PushStatus pushStatus;
    private PushWay pushWay;
    private Boolean readFlag;
    private String title;
    private Object content;
    private PushMsgType type;
    private Integer level;
    private String url;
    private Date createTime;
    private Map<String, Object> extend;
    private String testRunId;
    private Long clientSentAt;
}
