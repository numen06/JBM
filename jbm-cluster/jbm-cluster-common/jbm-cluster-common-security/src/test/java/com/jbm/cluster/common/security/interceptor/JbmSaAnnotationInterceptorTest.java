package com.jbm.cluster.common.security.interceptor;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.id.SaIdUtil;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JbmSaAnnotationInterceptorTest {

    @Test
    void preHandle_skipsSaCheckLogin_forTrustedInternalCall() throws Exception {
        TestInterceptor interceptor = new TestInterceptor();
        Method method = DemoController.class.getDeclaredMethod("securedEndpoint");
        HandlerMethod handlerMethod = new HandlerMethod(new DemoController(), method);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(JbmSecurityConstants.FROM_SOURCE, JbmSecurityConstants.INNER);
        request.addHeader(SaIdUtil.ID_TOKEN, "internal-id-token");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod);

        assertTrue(allowed);
        assertTrue(interceptor.internalTokenValidated);
    }

    @Test
    void preHandle_enforcesSaCheckLogin_forUntrustedCall() throws Exception {
        TestInterceptor interceptor = new TestInterceptor();
        Method method = DemoController.class.getDeclaredMethod("securedEndpoint");
        HandlerMethod handlerMethod = new HandlerMethod(new DemoController(), method);

        assertThrows(SaTokenException.class, () -> interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), handlerMethod));
    }

    @Test
    void preHandle_doesNotBypassPermissionForUserWithoutLoginState() throws Exception {
        TestInterceptor interceptor = new TestInterceptor();
        Method method = DemoController.class.getDeclaredMethod("permissionEndpoint");
        HandlerMethod handlerMethod = new HandlerMethod(new DemoController(), method);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer user-token-without-permissions");

        assertThrows(SaTokenException.class, () -> interceptor.preHandle(
                request, new MockHttpServletResponse(), handlerMethod));
    }

    static class TestInterceptor extends JbmSaAnnotationInterceptor {
        private boolean internalTokenValidated;

        @Override
        boolean isUserLoggedIn() {
            return false;
        }

        @Override
        void validateInternalToken() {
            internalTokenValidated = true;
        }
    }

    static class DemoController {
        @SaCheckLogin
        public void securedEndpoint() {
        }

        @SaCheckPermission("ACTION_monitor:online:list")
        public void permissionEndpoint() {
        }
    }
}
