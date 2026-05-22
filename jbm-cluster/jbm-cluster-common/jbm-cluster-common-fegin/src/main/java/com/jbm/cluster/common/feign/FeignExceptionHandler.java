package com.jbm.cluster.common.feign;

import com.jbm.framework.metadata.bean.ResultBody;
import jbm.framework.boot.autoconfigure.feign.RemoteServiceException;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(100)
public class FeignExceptionHandler {

    @ExceptionHandler(RemoteServiceException.class)
    public ResultBody<?> handleRemoteServiceException(RemoteServiceException ex) {
        if (ex.getCode() != null) {
            return ResultBody.error(ex.getCode(), ex.getMessage());
        }
        return ResultBody.error(ex.getMessage());
    }
}