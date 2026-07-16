package com.jbm.cluster.common.satoken.utils;

import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginHelperTest {

    @Test
    void shouldInvalidateSaTokenEvenWhenOAuthRevocationFails() {
        try (MockedStatic<StpUtil> stpUtil = Mockito.mockStatic(StpUtil.class);
             MockedStatic<SaOAuth2Util> oauth2Util = Mockito.mockStatic(SaOAuth2Util.class)) {
            stpUtil.when(StpUtil::getTokenValue).thenReturn("current-token");
            oauth2Util.when(() -> SaOAuth2Util.revokeAccessToken("current-token"))
                    .thenThrow(new IllegalStateException("oauth cleanup failed"));

            assertThrows(IllegalStateException.class, LoginHelper::loginout);

            stpUtil.verify(() -> StpUtil.logoutByTokenValue("current-token"));
        }
    }

    @Test
    void shouldRevokeOAuthTokenEvenWhenSaTokenLogoutFails() {
        try (MockedStatic<StpUtil> stpUtil = Mockito.mockStatic(StpUtil.class);
             MockedStatic<SaOAuth2Util> oauth2Util = Mockito.mockStatic(SaOAuth2Util.class)) {
            stpUtil.when(StpUtil::getTokenValue).thenReturn("current-token");
            stpUtil.when(() -> StpUtil.logoutByTokenValue("current-token"))
                    .thenThrow(new IllegalStateException("sa-token cleanup failed"));

            assertThrows(IllegalStateException.class, LoginHelper::loginout);

            oauth2Util.verify(() -> SaOAuth2Util.revokeAccessToken("current-token"));
        }
    }
}
