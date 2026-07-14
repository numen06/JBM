package com.jbm.cluster.common.feign;

import cn.dev33.satoken.id.SaIdUtil;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeignRequestInterceptorTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void copiesBusinessHeadersWithoutTrustingInboundSecurityHeaders() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBeansOfType(PreRequestInterceptor.class)).thenReturn(Collections.emptyMap());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.8");
        request.addHeader("X-Tenant-Id", "tenant-a");
        request.addHeader(JbmSecurityConstants.AUTHORIZATION_HEADER, "Bearer inbound-user-token");
        request.addHeader(SaIdUtil.ID_TOKEN, "inbound-id-token");
        request.addHeader(JbmSecurityConstants.FROM_SOURCE, JbmSecurityConstants.INNER);
        request.addHeader(FeignTokenContext.ACCESS_MODE_HEADER, FeignTokenContext.ACCESS_MODE_USER);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        template.header(FeignTokenContext.ACCESS_MODE_HEADER, FeignTokenContext.ACCESS_MODE_RELAY);
        FeignRequestInterceptor interceptor = new FeignRequestInterceptor();
        ReflectionTestUtils.setField(interceptor, "applicationContext", applicationContext);

        interceptor.apply(template);

        assertThat(template.headers().get("X-Tenant-Id")).containsExactly("tenant-a");
        assertThat(template.headers()).doesNotContainKey(JbmSecurityConstants.AUTHORIZATION_HEADER);
        assertThat(template.headers()).doesNotContainKey(SaIdUtil.ID_TOKEN);
        assertThat(template.headers()).doesNotContainKey(JbmSecurityConstants.FROM_SOURCE);
        assertThat(template.headers().get(FeignTokenContext.ACCESS_MODE_HEADER))
                .containsExactly(FeignTokenContext.ACCESS_MODE_RELAY);
    }

    @Test
    void delegatesFinalTokenDecisionToPreRequestInterceptor() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        PreRequestInterceptor tokenInterceptor = mock(PreRequestInterceptor.class);
        when(applicationContext.getBeansOfType(PreRequestInterceptor.class))
                .thenReturn(Collections.singletonMap("appPreRequestInterceptor", tokenInterceptor));

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        RequestTemplate template = new RequestTemplate();
        FeignRequestInterceptor interceptor = new FeignRequestInterceptor();
        ReflectionTestUtils.setField(interceptor, "applicationContext", applicationContext);

        interceptor.apply(template);

        verify(tokenInterceptor).apply(template, request);
    }
}
