/*
 * Copyright (c) 2024-present HiveMQ and the HiveMQ Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expres or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package jbm.framework.boot.autoconfigure.mqtt.hivemq.factories;

import cn.hutool.core.util.StrUtil;
import com.hivemq.client.mqtt.*;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.auth.Mqtt5EnhancedAuthMechanism;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserPropertiesBuilder;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5ConnectBuilder;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5ConnectRestrictions;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.config.HiveMqttProperties;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.ssl.KeyManagerFactoryCreationException;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.ssl.TrustManagerFactoryCreationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A factory to create an MQTT v5 client.
 *
 * @author Sven Kobow
 * @since 1.0.0
 */
@Slf4j
public final class Mqtt5ClientFactory implements IMqttClientFactory {

    /**
     * Creates a new instance of a {@link Mqtt5AsyncClient} with the given configuration.
     *
     * @param configuration         The configuration to apply.
     * @param enhancedAuthMechanism An optional implementation of {@link Mqtt5EnhancedAuthMechanism} to add enhanced authentication.
     * @return A new instance of {@link Mqtt5AsyncClient}
     */

    public Mqtt5AsyncClient mqttClient(final HiveMqttProperties configuration, @Nullable final Mqtt5EnhancedAuthMechanism enhancedAuthMechanism) {
        // ⚠️ 诊断日志：无论如何都输出，确认代码是否重新编译
        log.warn("🔧🔧🔧 MQTT5ClientFactory.mqttClient() called - CODE VERSION: 2025-10-27-v2");
        
        final Mqtt5ClientBuilder clientBuilder = MqttClient.builder()
                .useMqttVersion5()
                .transportConfig(buildTransportConfig(configuration));

        if (StrUtil.isNotBlank(configuration.getClientId())) {
            // 直接使用配置的Client ID，不添加前缀（避免超长导致某些MQTT服务器拒绝）
            String clientId = configuration.getClientId();
            clientBuilder.identifier(clientId);
            log.info("🔧 Creating MQTT5 client with ClientId: {} (length: {})", clientId, clientId.length());
        } else {
            log.warn("⚠️ ClientId is empty, MQTT client will use auto-generated ID");
        }
        if (configuration.isAutomaticReconnect()) {
            clientBuilder.automaticReconnect()
                    .initialDelay(1, TimeUnit.SECONDS) // 初始延迟1秒
                    .maxDelay(configuration.getMaxReconnectDelay(), TimeUnit.SECONDS) // 最大延迟
                    .applyAutomaticReconnect();
        }

        final Mqtt5ConnectBuilder connectBuilder = Mqtt5Connect.builder()
                .cleanStart(configuration.isCleanStart())
                .keepAlive(configuration.getKeepAliveInterval())
                .sessionExpiryInterval(configuration.getSessionExpiryInterval())
                .restrictions(Mqtt5ConnectRestrictions.builder()
                        .receiveMaximum(configuration.getReceiveMaximum())
                        .maximumPacketSize(configuration.getMaximumPacketSize())
                        .topicAliasMaximum(configuration.getTopicAliasMaximum())
                        .requestResponseInformation(configuration.isRequestResponseInfo())
                        .requestProblemInformation(configuration.isRequestProblemInfo())
                        .build());

        if (configuration.getUserProperties() != null && !configuration.getUserProperties().isEmpty()) {
            connectBuilder.userProperties(buildUserProperties(configuration));
        }

        if (enhancedAuthMechanism != null) {
            connectBuilder.enhancedAuth(enhancedAuthMechanism);
        }

        if (StrUtil.isNotEmpty(configuration.getUserName())) {
            connectBuilder.simpleAuth()
                    .username(configuration.getUserName())
                    .password(StrUtil.bytes(configuration.getPassword()))
                    .applySimpleAuth();
        }

        if (configuration.getWillMessage() != null) {
            HiveMqttProperties.WillMessageProperties willMessage = configuration.getWillMessage();

            connectBuilder.willPublish()
                    .topic(willMessage.getTopic())
                    .payload(willMessage.getPayload())
                    .qos(Objects.requireNonNull(MqttQos.fromCode(willMessage.getQos())))
                    .retain(willMessage.isRetained());
        }

        final Mqtt5AsyncClient client = clientBuilder
                .addConnectedListener(connectedEvent -> {
                    // 只在 DEBUG 级别记录连接成功，减少日志量
                    if (log.isDebugEnabled()) {
                        log.debug("✅ Connected to MQTT5 Client:{}", 
                                connectedEvent.getClientConfig().getClientIdentifier());
                    }
                }).addDisconnectedListener(disconnectedEvent -> {
                    Throwable cause = disconnectedEvent.getCause();
                    String clientId = disconnectedEvent.getClientConfig().getClientIdentifier().toString();
                    int attempts = disconnectedEvent.getReconnector().getAttempts();
                    
                    // 检测 Client ID 冲突
                    boolean isClientIdConflict = (attempts == 0 && cause != null && 
                            (cause.getMessage().contains("DISCONNECT") || 
                             cause.getMessage().contains("Session taken over")));
                    
                    if (isClientIdConflict) {
                        log.error("🚨 CRITICAL: Client ID conflict detected! ClientId:{}, Reason:{}", 
                                clientId, cause != null ? cause.getMessage() : "Unknown");
                        log.error("🚨 Another system is using the same Client ID. Please ensure Client ID is unique!");
                        log.error("🚨 Suggestion: Use spring.mqtt.client-id=${{spring.application.name}}-${{random.uuid}}");
                    } else if (attempts > 0) {
                        // 正常重连，输出详细原因便于排查
                        if (cause != null) {
                            log.warn("🔄 Disconnected (Reconnect attempts:{}), Reason: {}, will auto reconnect", 
                                    attempts, cause.getMessage());
                            // 如果有具体的异常堆栈，在DEBUG级别输出
                            if (log.isDebugEnabled()) {
                                log.debug("🔍 Disconnect detail:", cause);
                            }
                        } else {
                            log.info("🔄 Disconnected (Reconnect attempts:{}), will auto reconnect", attempts);
                        }
                    } else if (log.isDebugEnabled()) {
                        // 其他情况只在 DEBUG 级别记录
                        log.debug("❌ Disconnected: ClientId:{}, Attempts:{}, Reason:{}", 
                                clientId, attempts, cause != null ? cause.getMessage() : "Unknown");
                    }
                }).buildAsync();

        if (log.isTraceEnabled()) {
            log.trace("Connecting to {} on port {}", configuration.getServerHost(), configuration.getServerPort());
        }
        
        // 异步连接，避免阻塞启动流程
        client.connect(connectBuilder.build())
                .whenComplete((mqtt5ConnAck, throwable) -> {
                    if (throwable != null) {
                        log.error("❌ Initial connection failed for MQTT client, Server: {}:{}, ClientId: {}, Reason: {}", 
                                configuration.getServerHost(), 
                                configuration.getServerPort(),
                                configuration.getClientId(),
                                throwable.getMessage());
                        log.error("💡 Please check: 1) Network connectivity 2) Server address 3) Authentication 4) Firewall rules");
                        // DEBUG级别输出完整堆栈
                        if (log.isDebugEnabled()) {
                            log.debug("🔍 Connection failure detail:", throwable);
                        }
                    } else {
                        log.info("✅ MQTT client connected successfully: Server={}:{}, ClientId={}", 
                                configuration.getServerHost(),
                                configuration.getServerPort(),
                                configuration.getClientId());
                    }
                });

        return client;
    }

    public Mqtt5UserProperties buildUserProperties(final HiveMqttProperties configuration) {
        final Mqtt5UserPropertiesBuilder propertiesBuilder = Mqtt5UserProperties.builder();
        configuration.getUserProperties().forEach(propertiesBuilder::add);

        return propertiesBuilder.build();
    }

    private MqttClientTransportConfig buildTransportConfig(final HiveMqttProperties configuration) {

        final MqttClientTransportConfigBuilder transportConfigBuilder = MqttClientTransportConfig.builder()
                .serverHost(configuration.getServerHost())
                .serverPort(configuration.getServerPort())
                .mqttConnectTimeout(configuration.getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS);

        if (configuration.isSSL() && configuration.getSslProperties() != null) {
            final HiveMqttProperties.SslProperties certConfiguration = configuration.getSslProperties();
            final MqttClientSslConfigBuilder sslConfigBuilder = MqttClientSslConfig.builder();

            try {
                sslConfigBuilder
                        .keyManagerFactory(getKeyManagerFactory(certConfiguration))
                        .trustManagerFactory(getTrustManagerFactory(certConfiguration));

            } catch (KeyManagerFactoryCreationException | TrustManagerFactoryCreationException e) {
                throw new BeanInstantiationException(MqttClientTransportConfig.class, "Error creating SSL configuration", e);
            }

            transportConfigBuilder.sslConfig(sslConfigBuilder.build());
        }

        return transportConfigBuilder.build();
    }
}
