package com.jbm.cluster.api.model.auth;

import lombok.Data;

/**
 * @Created wesley.zhang
 * @Date 2022/5/3 20:55
 * @Description TODO
 */

@Data
public class AccessTokenResult {

    private String accessToken;

    private Long expiresIn;

    private String scope;

    private String tokenType = "Bearer";
}
