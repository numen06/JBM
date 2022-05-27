package com.jbm.cluster.common.satoken.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.jwt.exception.SaJwtException;
import jbm.framework.boot.autoconfigure.filter.SimpleUnknownRuntimeExceptionFilter;

import javax.naming.NoPermissionException;
import java.util.Set;

/**
 * @Created wesley.zhang
 * @Date 2022/5/21 19:46
 * @Description TODO
 */
public class SaUnknownRuntimeExceptionFilter extends SimpleUnknownRuntimeExceptionFilter {

    @Override
    public void filterRuntimeExceptions(Set<Class<? extends RuntimeException>> runtimeExceptions) {
        runtimeExceptions.add(SaTokenException.class);
    }
}
