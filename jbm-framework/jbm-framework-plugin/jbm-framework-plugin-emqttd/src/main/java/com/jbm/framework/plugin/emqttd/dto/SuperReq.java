package com.jbm.framework.plugin.emqttd.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-11-20 19:27
 **/
@Data
public class SuperReq implements Serializable {

    private String clientid;

    private String username;

}
