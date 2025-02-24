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
import com.hivemq.client.mqtt.lifecycle.MqttClientAutoReconnect;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.auth.Mqtt5EnhancedAuthMechanism;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserPropertiesBuilder;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5ConnectBuilder;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5ConnectRestrictions;
import jbm.framework.boot.autoconfigure.mqtt.exception.MqttClientException;
import jbm.framework.boot.autoconfigure.mqtt.hivemq.config.MqttProperties;
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
    public Mqtt5AsyncClient mqttClient(final MqttProperties configuration, @Nullable final Mqtt5EnhancedAuthMechanism enhancedAuthMechanism) {
        final Mqtt5ClientBuilder clientBuilder = MqttClient.builder()
                .useMqttVersion5()
                .transportConfig(buildTransportConfig(configuration));

        if (StrUtil.isNotBlank(configuration.getClientId())) {
            clientBuilder.identifier("hivemq_client_" + configuration.getClientId());
        }
        if (configuration.isAutomaticReconnect()) {
            clientBuilder.automaticReconnect()
                    .initialDelay(MqttClientAutoReconnect.DEFAULT_START_DELAY_S, TimeUnit.SECONDS)
                    .maxDelay(configuration.getMaxReconnectDelay(), TimeUnit.SECONDS)
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
                    .password(configuration.getPassword())
                    .applySimpleAuth();
        }

        if (configuration.getWillMessage() != null) {
            MqttProperties.WillMessageProperties willMessage = configuration.getWillMessage();

            connectBuilder.willPublish()
                    .topic(willMessage.getTopic())
                    .payload(willMessage.getPayload())
                    .qos(Objects.requireNonNull(MqttQos.fromCode(willMessage.getQos())))
                    .retain(willMessage.isRetained());
        }

        final Mqtt5AsyncClient client = clientBuilder.buildAsync();

        if (log.isTraceEnabled()) {
            log.trace("Connecting to {} on port {}", configuration.getServerHost(), configuration.getServerPort());
        }

        client.connect(connectBuilder.build())
                .whenComplete((mqtt3ConnAck, throwable) -> {
                    if (throwable != null) {
                        throw new MqttClientException("Error connecting mqtt client");
                    }
                }).join();

        return client;
    }

    public Mqtt5UserProperties buildUserProperties(final MqttProperties configuration) {
        final Mqtt5UserPropertiesBuilder propertiesBuilder = Mqtt5UserProperties.builder();
        configuration.getUserProperties().forEach(propertiesBuilder::add);

        return propertiesBuilder.build();
    }

    private MqttClientTransportConfig buildTransportConfig(final MqttProperties configuration) {

        final MqttClientTransportConfigBuilder transportConfigBuilder = MqttClientTransportConfig.builder()
                .serverHost(configuration.getServerHost())
                .serverPort(configuration.getServerPort())
                .mqttConnectTimeout(configuration.getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS);

        if (configuration.isSSL() && configuration.getSslProperties() != null) {
            final MqttProperties.SslProperties certConfiguration = configuration.getSslProperties();
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
