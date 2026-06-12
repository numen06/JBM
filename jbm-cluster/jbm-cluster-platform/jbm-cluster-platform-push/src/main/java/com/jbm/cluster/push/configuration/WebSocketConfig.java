package com.jbm.cluster.push.configuration;

import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    JbmLoginUser loginUser = resolveLoginUser(accessor.getFirstNativeHeader("Authorization"));
                    if (loginUser == null || loginUser.getUserId() == null) {
                        throw new IllegalArgumentException("WebSocket连接未授权");
                    }
                    accessor.setUser(new UserPrincipal(String.valueOf(loginUser.getUserId())));
                    if (accessor.getSessionAttributes() != null) {
                        accessor.getSessionAttributes().put("userId", loginUser.getUserId());
                    }
                    log.info("WebSocket连接建立 userId={}, session={}", loginUser.getUserId(), accessor.getSessionId());
                }
                return message;
            }
        });
    }

    private JbmLoginUser resolveLoginUser(String authorization) {
        String token = cleanToken(authorization);
        if (StrUtil.isBlank(token)) {
            return null;
        }
        try {
            JbmLoginUser user = LoginHelper.getLoginUser(token);
            if (user != null) {
                return user;
            }
        } catch (Exception ignored) {
        }
        try {
            Object loginId = SaOAuth2Util.getLoginIdByAccessToken(token);
            if (loginId != null) {
                return LoginHelper.getLoginUser(loginId);
            }
        } catch (Exception ignored) {
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId != null) {
                return LoginHelper.getLoginUser(loginId);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String cleanToken(String authorization) {
        if (StrUtil.isBlank(authorization)) {
            return authorization;
        }
        String token = authorization.trim();
        if (StrUtil.startWithIgnoreCase(token, "Bearer ")) {
            return token.substring("Bearer ".length()).trim();
        }
        return token;
    }

    private static class UserPrincipal implements Principal {
        private final String name;

        UserPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
