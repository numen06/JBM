package com.jbm.framework.plugin.emqttd.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-11-20 19:22
 **/
@Data
public class AuthReq implements Serializable {

    private String clientid;

    private String username;

    private String password;
}
