package com.jbm.cluster.auth.controller;

import cn.hutool.core.exceptions.ValidateException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.metadata.enumerate.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuth2ServerControllerTest {

    @Test
    void shouldReturnCaptchaValidationAsBusinessError() throws Exception {
        OAuth2ServerController controller = new OAuth2ServerController();

        ResultBody<Void> result = controller.handleValidateException(new ValidateException("验证码错误"));

        assertFalse(result.getSuccess());
        assertEquals("验证码错误", result.getMessage());
        assertEquals(ErrorCode.FAIL.getCode(), result.getCode());

        Method handler = OAuth2ServerController.class
                .getMethod("handleValidateException", ValidateException.class);
        ExceptionHandler annotation = handler.getAnnotation(ExceptionHandler.class);
        assertTrue(Arrays.asList(annotation.value()).contains(ValidateException.class));
    }
}
