package com.jbm.cluster.common.satoken.oauth;

import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class JbmNodeOAuth2TemplateImplTest {

    @Test
    void applicationClientShouldBypassNodeTokenCache() {
        AtomicInteger generatedCount = new AtomicInteger();
        JbmNodeOAuth2TemplateImpl template = new JbmNodeOAuth2TemplateImpl() {
            @Override
            protected ClientTokenModel generateOAuthClientToken(String clientId, String scope) {
                int sequence = generatedCount.incrementAndGet();
                return new ClientTokenModel("application-token-" + sequence, clientId, scope);
            }
        };

        try (MockedStatic<SpringUtil> springUtil = Mockito.mockStatic(SpringUtil.class)) {
            springUtil.when(SpringUtil::getApplicationName).thenReturn("jbm-auth-service");

            ClientTokenModel first = template.generateClientToken("external-client", "read");
            ClientTokenModel second = template.generateClientToken("external-client", "read");

            assertEquals(2, generatedCount.get());
            assertEquals("external-client", first.clientId);
            assertEquals("external-client", second.clientId);
            assertNotEquals(first.clientToken, second.clientToken);
        }
    }

    @Test
    void randomClientTokenShouldNotReuseGlobalIdToken() {
        JbmNodeOAuth2TemplateImpl template = new JbmNodeOAuth2TemplateImpl();

        String first = template.randomClientToken("external-client", "*");
        String second = template.randomClientToken("external-client", "*");

        assertNotEquals(first, second);
    }
}
