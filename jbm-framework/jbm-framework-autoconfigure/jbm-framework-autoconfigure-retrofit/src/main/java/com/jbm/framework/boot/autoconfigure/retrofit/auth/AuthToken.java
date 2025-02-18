package com.jbm.framework.boot.autoconfigure.retrofit.auth;

import lombok.Data;

import java.util.Date;

/**
 * @author wesley
 */
@Data
public class AuthToken {

    // 令牌
    private String token;
    // 过期时间
    private Long expireTime;
    // 令牌类型
    private String type;
    // 刷新令牌
    private Date createTime;

}
