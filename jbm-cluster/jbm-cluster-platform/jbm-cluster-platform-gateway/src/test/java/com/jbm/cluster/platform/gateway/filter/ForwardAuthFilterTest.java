package com.jbm.cluster.platform.gateway.filter;

import cn.dev33.satoken.id.SaIdUtil;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ForwardAuthFilterTest {

    @Test
    void shouldPreserveAuthorizationAndStripInternalHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/base/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
                .header(SaIdUtil.ID_TOKEN, "attacker-id-token")
                .header(JbmSecurityConstants.FROM_SOURCE, JbmSecurityConstants.INNER)
                .build();
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        };
        ForwardAuthFilter filter = new ForwardAuthFilter();

        filter.filter(MockServerWebExchange.from(request), chain).block();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertEquals("Bearer user-token", headers.getFirst(HttpHeaders.AUTHORIZATION));
        assertFalse(headers.containsKey(SaIdUtil.ID_TOKEN));
        assertFalse(headers.containsKey(JbmSecurityConstants.FROM_SOURCE));
        assertEquals("/base/test", headers.getFirst("X-Original-Path"));
    }
}
