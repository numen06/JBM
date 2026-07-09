package com.jbm.cluster.common.security.interceptor;

import com.jbm.cluster.common.security.context.InnerAuthContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JbmSaAnnotationInterceptorTest {

    private final JbmSaAnnotationInterceptor interceptor = new JbmSaAnnotationInterceptor();

    @AfterEach
    void tearDown() {
        InnerAuthContext.clear();
    }

    @Test
    void preHandle_skipsSaAnnotationWhenInnerAuthValidatedWithoutUser() throws Exception {
        InnerAuthContext.setSkipPermissionCheck(true);

        boolean allowed = interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());

        assertTrue(allowed);
    }
}
