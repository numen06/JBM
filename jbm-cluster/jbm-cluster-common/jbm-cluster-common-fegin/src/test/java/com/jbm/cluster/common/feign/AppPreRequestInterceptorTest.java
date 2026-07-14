package com.jbm.cluster.common.feign;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.id.SaIdUtil;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.hutool.extra.spring.SpringUtil;
import com.jbm.cluster.common.feign.annotation.FeignTokenRelay;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import feign.MethodMetadata;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AppPreRequestInterceptorTest {

    private interface AnnotatedFeignClient {

        @FeignTokenRelay
        @GetMapping("/downstream")
        void call();
    }

    private AppPreRequestInterceptor interceptor;
    private SaOAuth2Template saOAuth2Template;
    private MockedStatic<SpringUtil> springUtilMock;
    private MockedStatic<SaIdUtil> saIdUtilMock;

    @BeforeEach
    void setUp() {
        FeignTokenContext.clear();
        interceptor = new AppPreRequestInterceptor();
        saOAuth2Template = mock(SaOAuth2Template.class);
        ReflectionTestUtils.setField(interceptor, "saOAuth2Template", saOAuth2Template);
        springUtilMock = Mockito.mockStatic(SpringUtil.class);
        springUtilMock.when(SpringUtil::getApplicationName).thenReturn("test-service");
        saIdUtilMock = Mockito.mockStatic(SaIdUtil.class);
        saIdUtilMock.when(SaIdUtil::getToken).thenReturn("mock-id-token");
    }

    @AfterEach
    void tearDown() {
        FeignTokenContext.clear();
        springUtilMock.close();
        saIdUtilMock.close();
    }

    @Test
    void shouldOverrideInboundAuthorization_whenRelayNotRequested() {
        ClientTokenModel clientTokenModel = new ClientTokenModel("client-token-value", "test-service", "*");
        when(saOAuth2Template.generateClientToken(anyString(), anyString())).thenReturn(clientTokenModel);
        SaManager.getConfig().setTokenPrefix("Bearer");

        RequestTemplate template = new RequestTemplate();
        template.header(JbmSecurityConstants.AUTHORIZATION_HEADER, "Bearer inbound-token");

        interceptor.apply(template, null);

        assertFalse(template.headers().get(JbmSecurityConstants.AUTHORIZATION_HEADER).contains("Bearer inbound-token"));
        assertTrue(template.headers().get(JbmSecurityConstants.AUTHORIZATION_HEADER).contains("Bearer client-token-value"));
        assertTrue(template.headers().containsKey(SaIdUtil.ID_TOKEN));
        assertTrue(template.headers().containsKey(JbmSecurityConstants.FROM_SOURCE));
    }

    @Test
    void shouldGenerateClientToken_whenAuthorizationMissing() {
        ClientTokenModel clientTokenModel = new ClientTokenModel("client-token-value", "test-service", "*");
        when(saOAuth2Template.generateClientToken(anyString(), anyString())).thenReturn(clientTokenModel);
        SaManager.getConfig().setTokenPrefix("Bearer");

        RequestTemplate template = new RequestTemplate();
        HttpServletRequest request = mock(HttpServletRequest.class);

        interceptor.apply(template, request);

        String prefix = SaManager.getConfig().getTokenPrefix();
        assertTrue(template.headers().get(JbmSecurityConstants.AUTHORIZATION_HEADER)
                .contains(prefix + " client-token-value"));
    }

    @Test
    void shouldGenerateClientToken_whenNoHttpRequest() {
        ClientTokenModel clientTokenModel = new ClientTokenModel("client-token-value", "test-service", "*");
        when(saOAuth2Template.generateClientToken(anyString(), anyString())).thenReturn(clientTokenModel);
        SaManager.getConfig().setTokenPrefix("Bearer");

        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template, null);

        assertFalse(template.headers().getOrDefault(JbmSecurityConstants.AUTHORIZATION_HEADER, Collections.emptyList()).isEmpty());
    }

    @Test
    void shouldRelayInboundAuthorization_whenContextRequested() {
        RequestTemplate template = new RequestTemplate();
        template.header(JbmSecurityConstants.AUTHORIZATION_HEADER, "Bearer upstream-client-token");

        FeignTokenContext.withTokenRelay(() -> interceptor.apply(template, null));

        assertTrue(template.headers().get(JbmSecurityConstants.AUTHORIZATION_HEADER).contains("Bearer upstream-client-token"));
        assertFalse(template.headers().containsKey(SaIdUtil.ID_TOKEN));
        assertFalse(template.headers().containsKey(JbmSecurityConstants.FROM_SOURCE));
        assertFalse(FeignTokenContext.isTokenRelay());
    }

    @Test
    void shouldRelayInboundAuthorization_whenHeaderRequested() {
        RequestTemplate template = new RequestTemplate();
        template.header(JbmSecurityConstants.AUTHORIZATION_HEADER, "Bearer inbound-token");
        template.header(FeignTokenContext.ACCESS_MODE_HEADER, FeignTokenContext.ACCESS_MODE_RELAY);

        interceptor.apply(template, null);

        assertTrue(template.headers().get(JbmSecurityConstants.AUTHORIZATION_HEADER).contains("Bearer inbound-token"));
        assertFalse(template.headers().containsKey(FeignTokenContext.ACCESS_MODE_HEADER));
        assertFalse(template.headers().containsKey(SaIdUtil.ID_TOKEN));
        assertFalse(template.headers().containsKey(JbmSecurityConstants.FROM_SOURCE));
    }

    @Test
    void shouldRelayAuthorizationFromCurrentHttpRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(JbmSecurityConstants.AUTHORIZATION_HEADER, "Bearer gateway-inbound-token");
        RequestTemplate template = new RequestTemplate();
        template.header(FeignTokenContext.ACCESS_MODE_HEADER, FeignTokenContext.ACCESS_MODE_RELAY);

        interceptor.apply(template, request);

        assertTrue(template.headers().get(JbmSecurityConstants.AUTHORIZATION_HEADER)
                .contains("Bearer gateway-inbound-token"));
        assertFalse(template.headers().containsKey(FeignTokenContext.ACCESS_MODE_HEADER));
        assertFalse(template.headers().containsKey(SaIdUtil.ID_TOKEN));
        assertFalse(template.headers().containsKey(JbmSecurityConstants.FROM_SOURCE));
        verifyNoInteractions(saOAuth2Template);
    }

    @Test
    void shouldExposeRelayHeaderFromAnnotation() throws NoSuchMethodException {
        Method method = AnnotatedFeignClient.class.getDeclaredMethod("call");
        MethodMetadata metadata = new SpringMvcContract()
                .parseAndValidateMetadata(AnnotatedFeignClient.class, method);

        assertTrue(metadata.template().headers().get(FeignTokenContext.ACCESS_MODE_HEADER)
                .contains(FeignTokenContext.ACCESS_MODE_RELAY));
    }

    @Test
    void shouldAcceptLegacyUserMarkerAsTokenRelay() {
        RequestTemplate template = new RequestTemplate();
        template.header(JbmSecurityConstants.AUTHORIZATION_HEADER, "Bearer inbound-token");
        template.header(FeignTokenContext.ACCESS_MODE_HEADER, "user");

        interceptor.apply(template, null);

        assertTrue(template.headers().get(JbmSecurityConstants.AUTHORIZATION_HEADER).contains("Bearer inbound-token"));
        assertFalse(template.headers().containsKey(FeignTokenContext.ACCESS_MODE_HEADER));
        verifyNoInteractions(saOAuth2Template);
    }

    @Test
    void shouldNotFallbackToInternalToken_whenRelayRequestedWithoutAuthorization() {
        RequestTemplate template = new RequestTemplate();
        template.header(FeignTokenContext.ACCESS_MODE_HEADER, FeignTokenContext.ACCESS_MODE_RELAY);

        interceptor.apply(template, null);

        assertFalse(template.headers().containsKey(JbmSecurityConstants.AUTHORIZATION_HEADER));
        assertFalse(template.headers().containsKey(SaIdUtil.ID_TOKEN));
        assertFalse(template.headers().containsKey(JbmSecurityConstants.FROM_SOURCE));
        verifyNoInteractions(saOAuth2Template);
    }
}
