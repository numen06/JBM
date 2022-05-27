package com.jbm.cluster.common.satoken.core;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;

/**
 * @Created wesley.zhang
 * @Date 2022/5/21 17:40
 * @Description TODO
 */
public class UserLogicJwt extends StpLogicJwtForSimple {

    public UserLogicJwt() {
        super("user");
    }
}
