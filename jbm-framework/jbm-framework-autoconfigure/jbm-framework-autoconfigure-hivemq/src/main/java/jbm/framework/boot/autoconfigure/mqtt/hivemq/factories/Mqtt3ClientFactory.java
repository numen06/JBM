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
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3Connect;
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3ConnectBuilder;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.config.HiveMqttProperties;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.ssl.KeyManagerFactoryCreationException;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.ssl.TrustManagerFactoryCreationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanInstantiationException;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A factory to create an MQTT v3 client.
 *
 * @author Sven Kobow
 * @since 1.0.0
 */
@Slf4j
public final class Mqtt3ClientFactory implements IMqttClientFactory {

    public Mqtt3AsyncClient mqttClient(final HiveMqttProperties configuration) {

        final Mqtt3ClientBuilder clientBuilder = MqttClient.builder()
                .useMqttVersion3()
                .identifier(configuration.getClientId())
                .transportConfig(buildTransportConfig(configuration));

        if (configuration.isAutomaticReconnect()) {
            clientBuilder.automaticReconnect()
                    .initialDelay(1, TimeUnit.SECONDS) // 初始延迟1秒
                    .maxDelay(configuration.getMaxReconnectDelay(), TimeUnit.SECONDS) // 最大延迟
                    .applyAutomaticReconnect();
        }

        final Mqtt3ConnectBuilder connectBuilder = Mqtt3Connect.builder()
                .cleanSession(configuration.isCleanSession())
                .keepAlive(configuration.getKeepAliveInterval());

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

        final Mqtt3AsyncClient client = clientBuilder
                .addConnectedListener(connectedEvent -> {
                    log.info("✅ Connected or Reconnected to MQTT3 Broker");
                }).addDisconnectedListener(disconnectedEvent -> {
                    Throwable cause = disconnectedEvent.getCause();
                    if (cause != null) {
                        log.warn("❌ Disconnected from MQTT3 Broker, Reconnect attempts:{}, Reason:{}",
                                disconnectedEvent.getReconnector().getAttempts(),
                                cause.getMessage()
                        );
                    } else {
                        log.warn("❌ Disconnected from MQTT3 Broker, Reconnect attempts:{}",
                                disconnectedEvent.getReconnector().getAttempts()
                        );
                    }
                    // 不手动触发重连，让自动重连机制处理
                    // 手动触发会干扰自动重连机制，导致 attempts 始终为 0
                }).buildAsync();

        if (log.isTraceEnabled()) {
            log.trace("Connecting to {} on port {}", configuration.getServerHost(), configuration.getServerPort());
        }

        // 异步连接，避免阻塞启动流程
        client.connect(connectBuilder.build())
                .whenComplete((mqtt3ConnAck, throwable) -> {
                    if (throwable != null) {
                        log.error("❌ Initial connection failed for MQTT3 client, Server: {}:{}, ClientId: {}, Reason: {}", 
                                configuration.getServerHost(),
                                configuration.getServerPort(),
                                configuration.getClientId(),
                                throwable.getMessage());
                        log.error("💡 Please check: 1) Network connectivity 2) Server address 3) Authentication 4) Firewall rules");
                        if (log.isDebugEnabled()) {
                            log.debug("🔍 Connection failure detail:", throwable);
                        }
                        // 不抛出异常，让自动重连机制处理
                    } else {
                        log.info("✅ Initial connection successful for MQTT3 client, Server={}:{}, ClientId={}", 
                                configuration.getServerHost(),
                                configuration.getServerPort(),
                                configuration.getClientId());
                    }
                });

        return client;
    }

    private MqttClientTransportConfig buildTransportConfig(final HiveMqttProperties configuration) {

        final MqttClientTransportConfigBuilder transportConfigBuilder = MqttClientTransportConfig.builder()
                .serverHost(configuration.getServerHost())
                .serverPort(configuration.getServerPort())
                .mqttConnectTimeout(configuration.getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS);

        if (configuration.isSSL() && configuration.getSslProperties() != null) {
            final HiveMqttProperties.SslProperties sslConfiguration = configuration.getSslProperties();
            final MqttClientSslConfigBuilder sslConfigBuilder = MqttClientSslConfig.builder();

            try {
                sslConfigBuilder.trustManagerFactory(getTrustManagerFactory(sslConfiguration));

                if (sslConfiguration.getCertificate() != null) {
                    sslConfigBuilder.keyManagerFactory(getKeyManagerFactory(sslConfiguration));
                }

            } catch (KeyManagerFactoryCreationException |
                     TrustManagerFactoryCreationException e) {
                throw new BeanInstantiationException(MqttClientTransportConfig.class, "Error creating SSL configuration", e);
            }
        }

        return transportConfigBuilder.build();
    }
}
