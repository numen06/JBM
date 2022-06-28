package com.jbm.cluster.auth.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class AuthConfirmModel implements Serializable {


    /**
     * 随机验证码
     */
    private String state;
    /**
     * 确认码
     */
    private String code;
    /**
     * 登录token
     */
    private String token;

    /**
     * 客户端
     */
    private String clientId;

    /**
     * 设备信息
     */
    private String device;

    /**
     * 确认码的状态:0=创建；1=处理中；2=已确认
     */
    private Integer comfirmState;
}
