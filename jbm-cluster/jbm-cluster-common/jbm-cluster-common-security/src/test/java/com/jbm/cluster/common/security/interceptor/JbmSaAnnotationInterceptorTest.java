package com.jbm.cluster.common.security.interceptor;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JbmSaAnnotationInterceptorTest {

    private final JbmSaAnnotationInterceptor interceptor = new JbmSaAnnotationInterceptor();

    @Test
    void preHandle_skipsSaCheckLogin_whenNotLoggedIn() throws Exception {
        try (MockedStatic<StpUtil> stpUtilMock = Mockito.mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::isLogin).thenReturn(false);

            Method method = DemoController.class.getDeclaredMethod("securedEndpoint");
            HandlerMethod handlerMethod = new HandlerMethod(new DemoController(), method);

            boolean allowed = interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), handlerMethod);

            assertTrue(allowed);
        }
    }

    static class DemoController {
        @SaCheckLogin
        public void securedEndpoint() {
        }
    }
}
