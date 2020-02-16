package com.jbm.framework.plugin.emqttd.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-11-20 19:25
 **/
@Data
public class AclReq implements Serializable {

    private String access;
    private String username;
    private String clientid;
    private String ipaddr;
    private String topic;
}
