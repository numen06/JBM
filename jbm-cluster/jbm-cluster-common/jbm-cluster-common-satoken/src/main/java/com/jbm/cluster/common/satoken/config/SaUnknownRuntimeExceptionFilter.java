package com.jbm.cluster.common.satoken.config;

import cn.dev33.satoken.exception.SaTokenException;
import com.jbm.framework.exceptions.auth.NotLoginException;
import com.jbm.framework.exceptions.user.UserException;
import jbm.framework.web.exception.filter.SimpleUnknownRuntimeExceptionFilter;

import java.util.Set;

/**
 * @Created wesley.zhang
 * @Date 2022/5/21 19:46
 * @Description TODO
 */
public class SaUnknownRuntimeExceptionFilter extends SimpleUnknownRuntimeExceptionFilter {

    @Override
    public void filterRuntimeExceptions(Set<Class<? extends RuntimeException>> runtimeExceptions) {
        runtimeExceptions.add(NotLoginException.class);
        runtimeExceptions.add(SaTokenException.class);
        runtimeExceptions.add(UserException.class);
    }
}
