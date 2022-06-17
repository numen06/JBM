package com.jbm.cluster.common.basic.configuration;

import cn.hutool.core.exceptions.ValidateException;
import jbm.framework.boot.autoconfigure.filter.SimpleUnknownRuntimeExceptionFilter;

import javax.validation.ValidationException;
import java.util.Set;

/**
 * 通用的错误拦截
 *
 * @Created wesley.zhang
 * @Date 2022/5/21 19:46
 * @Description TODO
 */
public class BasicUnknownRuntimeExceptionFilter extends SimpleUnknownRuntimeExceptionFilter {

    @Override
    public void filterRuntimeExceptions(Set<Class<? extends RuntimeException>> runtimeExceptions) {
        runtimeExceptions.add(ValidateException.class);
        runtimeExceptions.add(ValidationException.class);
    }
}
