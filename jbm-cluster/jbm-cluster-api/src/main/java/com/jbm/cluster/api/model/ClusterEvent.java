package com.jbm.cluster.api.model;

import cn.hutool.core.net.NetUtil;
import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.util.Date;
import java.util.UUID;

/**
 * 集群事件
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ClusterEvent extends ApplicationEvent {

    private String eventId;

    private String topic;

    private Date createTime;

    private Date reviceTime;

    public ClusterEvent() {
        this("JBM Cluster Event");
    }

    public ClusterEvent(Object source) {
        super(source);
    }

    public void sendBuild() {
        this.eventId = UUID.randomUUID().toString();
        this.createTime = new Date();
    }

    public void reviceBuild() {
        if (this.source == null)
            this.source = this.eventId;
        this.reviceTime = new Date();
    }

    public String toJson() {
        return JSON.toJSONString(this);
    }


}
