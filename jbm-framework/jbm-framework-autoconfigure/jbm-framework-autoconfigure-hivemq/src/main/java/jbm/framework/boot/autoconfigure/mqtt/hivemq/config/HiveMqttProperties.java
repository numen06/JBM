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

package jbm.framework.boot.autoconfigure.mqtt.hivemq.config;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientTransportConfig;
import com.hivemq.client.mqtt.lifecycle.MqttClientAutoReconnect;
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3Connect;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5Connect;
import com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5ConnectRestrictions;
import lombok.Data;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * Configuration properties for HiveMQ MQTT client.
 *
 * @author Sven Kobow
 * @since 1.0.0
 */
@Data
public class HiveMqttProperties {
    private URI url = URI.create(String.format("tcp://%s:%s", MqttClient.DEFAULT_SERVER_HOST, MqttClient.DEFAULT_SERVER_PORT));
    private String clientId = null;
    private int mqttVersion = 5;
    private Duration connectionTimeout = Duration.ofSeconds(10); // 优化：10秒连接超时
    private Boolean manualAcks = false;
    private String password = null;
    private String userName = null;
    private Integer keepAliveInterval = 60; // 优化：60秒心跳（弱网环境，避免频繁心跳超时）
    private Long maxReconnectDelay = 120L; // 优化：最大重连延迟120秒（持续尝试重连）
    private boolean automaticReconnect = true;
    private Map<String, String> customWebSocketHeaders = null;
    private WillMessageProperties willMessage = null;
    private SslProperties sslProperties = null;
    private boolean cleanSession = Mqtt3Connect.DEFAULT_CLEAN_SESSION;
    /**
     * 默认 true：每次连接使用全新会话，避免 Broker 在订阅完成前推送会话恢复消息导致 "No publish flow registered"。
     * 若需离线消息缓冲，可配置 spring.mqtt.clean-start=false。
     */
    private boolean cleanStart = true;
    private Long sessionExpiryInterval = 86400L; // 优化：会话保持24小时（支持长时间断连）
    private Integer receiveMaximum = Mqtt5ConnectRestrictions.DEFAULT_RECEIVE_MAXIMUM;
    private Integer maximumPacketSize = Mqtt5ConnectRestrictions.DEFAULT_MAXIMUM_PACKET_SIZE;
    private Integer topicAliasMaximum = Mqtt5ConnectRestrictions.DEFAULT_TOPIC_ALIAS_MAXIMUM;
    private boolean requestResponseInfo = Mqtt5ConnectRestrictions.DEFAULT_REQUEST_RESPONSE_INFORMATION;
    private boolean requestProblemInfo = Mqtt5ConnectRestrictions.DEFAULT_REQUEST_PROBLEM_INFORMATION;
    private Map<String, String> userProperties;

    @Data
    public static class WillMessageProperties {

        private String topic;
        private byte[] payload;
        private int qos;
        private boolean retained;
    }

    @Data
    public static class SslProperties {
        private File certificateAuthority;
        private File certificate;
        private File privateKey;
        private char[] password;
    }

    /**
     * @return the server host of the configured {@link #url}.
     */
    public String getServerHost() {
        return url != null ? url.getHost() : null;
    }

    /**
     * @return the server port of the configured {@link #url}.
     */
    public Integer getServerPort() {
        return url != null ? url.getPort() : null;
    }

    public boolean isSSL() {
        return url != null && "SSL".equalsIgnoreCase(url.getScheme());
    }
}
