package com.jbm.cluster.auth.model;

import com.jbm.cluster.api.constants.LoginType;
import lombok.Data;

@Data
public class LoginProcessModel {

    private String username;
    private String originalPassword;
    private String decryptPassword;
    private String clientId;
    private String loginTypeValue;
    private LoginType loginType;
    /**
     * 登录设备
     */
    private String loginDevice;
    private String vcode;
}
