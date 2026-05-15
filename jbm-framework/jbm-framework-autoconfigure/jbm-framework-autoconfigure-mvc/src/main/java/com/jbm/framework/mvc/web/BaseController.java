package com.jbm.framework.mvc.web;

import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import java.util.Locale;

/**
 * 轻量 Controller 基类：仅提供统一响应与国际化辅助，不包含任何 CRUD 路由。
 */
public abstract class BaseController {

    @Autowired(required = false)
    protected MessageSource messageSource;

    protected <T> ResultBody<T> success(T data) {
        return ResultBody.ok(data);
    }

    protected <T> ResultBody<T> success(T data, String message) {
        return ResultBody.success(data, message);
    }

    protected <T> ResultBody<T> fail(String message) {
        return ResultBody.error(null, message);
    }

    protected String message(String code, Object... args) {
        if (messageSource == null) {
            return code;
        }
        return messageSource.getMessage(code, args, Locale.getDefault());
    }
}
