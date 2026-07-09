package com.jbm.cluster.common.satoken.core.filter;

import cn.dev33.satoken.oauth2.exception.SaOAuth2Exception;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.dev33.satoken.stp.StpUtil;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SaOAuthFilterAuthStrategyTest {

    private SaOAuthFilterAuthStrategy strategy;
    private MockedStatic<StpUtil> stpUtilMock;
    private MockedStatic<SaOAuth2Util> saOAuth2UtilMock;

    @BeforeEach
    void setUp() {
        strategy = new SaOAuthFilterAuthStrategy();
        JbmClusterProperties properties = new JbmClusterProperties();
        properties.setAllowLocalBypass(false);
        ReflectionTestUtils.setField(strategy, "jbmClusterProperties", properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer service-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        stpUtilMock = Mockito.mockStatic(StpUtil.class);
        saOAuth2UtilMock = Mockito.mockStatic(SaOAuth2Util.class);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        stpUtilMock.close();
        saOAuth2UtilMock.close();
    }

    @Test
    void shouldAcceptClientToken_withoutIdToken() {
        stpUtilMock.when(StpUtil::getTokenValue).thenReturn("service-token");
        stpUtilMock.when(StpUtil::getTokenInfo).thenReturn(null);
        saOAuth2UtilMock.when(() -> SaOAuth2Util.getAccessToken("service-token")).thenReturn(null);
        ClientTokenModel clientTokenModel = new ClientTokenModel("service-token", "test-service", "*");
        saOAuth2UtilMock.when(() -> SaOAuth2Util.getClientToken("service-token")).thenReturn(clientTokenModel);
        saOAuth2UtilMock.when(() -> SaOAuth2Util.checkClientToken("service-token")).thenReturn(clientTokenModel);

        assertDoesNotThrow(() -> strategy.run(null));
    }

    @Test
    void shouldReject_whenTokenMissing() {
        stpUtilMock.when(StpUtil::getTokenValue).thenReturn(null);

        assertThrows(SaOAuth2Exception.class, () -> strategy.run(null));
    }
}
