package jbm.framework.boot.autoconfigure.emqx;


import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5SimpleAuth;
import jbm.framework.boot.autoconfigure.emqx.configuration.EmqxMqttProperties;
import jbm.framework.boot.autoconfigure.emqx.event.EmqxClientEvent;
import jbm.framework.boot.autoconfigure.emqx.model.EmqxClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;

import java.util.concurrent.TimeUnit;

/**
 * @author wesley
 */
@Slf4j
public class EmqxApiListener implements InitializingBean {

    private final EmqxMqttProperties emqxMqttProperties;
    private final ApplicationEventPublisher eventPublisher;
    private Mqtt5Client client;

    public EmqxApiListener(EmqxMqttProperties emqxMqttProperties, ApplicationEventPublisher eventPublisher) {
        this.emqxMqttProperties = emqxMqttProperties;
        this.eventPublisher = eventPublisher;
    }

    private void initMqttClient() {
        //url是tcp://192.168.1.1:1883这种格式
        String host = StrUtil.subBetween(emqxMqttProperties.getUrl(), "//", ":");
        int port = NumberUtil.parseInt(StrUtil.subAfter(emqxMqttProperties.getUrl(), ":", true));
        this.client = Mqtt5Client.builder()
                .identifier("emqx-api-listener")
                .serverHost(host)
                .serverPort(port)
                .simpleAuth(Mqtt5SimpleAuth.builder().username(emqxMqttProperties.getUsername()).password(emqxMqttProperties.getPassword().getBytes()).build())
                .automaticReconnect()
                .initialDelay(1, TimeUnit.SECONDS)
                .maxDelay(60, TimeUnit.SECONDS)
                .applyAutomaticReconnect()
                .build();
    }

    private void connectAndSubscribe() {
        client.toAsync().connect()
                .thenAccept(connAck ->
                {
                    log.info(" 🚀 Connected to EMQX MQTT Broker");
                })
                .thenCompose(connAck -> client.toAsync().subscribeWith()
                        .topicFilter("$SYS/brokers/+/clients/+/connected")
                        .callback(publish -> {
                            //$SYS/brokers/emqx@172.17.0.4/clients/hivemq_IPrintService_078d5b3d9ed24e0ebfbe26715ff0f537/connected
                            String topic = publish.getTopic().toString();
                            String clientid = extractClientid(topic);
                            String payload = new String(publish.getPayloadAsBytes());
                            log.info(" 🔌 Client Online: " + clientid);
                            EmqxClient emqxClient = JSONObject.parseObject(payload, EmqxClient.class);
                            emqxClient.setConnected(true);
                            // 更新数据库状态
                            publishDeviceEvent(emqxClient, EmqxClientStatus.ONLINE);
                        })
                        .send())

                .thenCompose(subAck -> client.toAsync().subscribeWith()
                        .topicFilter("$SYS/brokers/+/clients/+/disconnected")
                        .callback(publish -> {
                            String topic = publish.getTopic().toString();
                            String clientid = extractClientid(topic);
                            String payload = new String(publish.getPayloadAsBytes());
                            log.info(" 📴 Client Offline: " + clientid);
                            EmqxClient emqxClient = JSONObject.parseObject(payload, EmqxClient.class);
                            emqxClient.setConnected(false);
                            publishDeviceEvent(emqxClient, EmqxClientStatus.OFFLINE);
                        })
                        .send())

                .exceptionally(throwable -> {
                    log.error("❌ 连接系统主题失败，请设置系统主题权限: ", throwable);
                    return null;
                });
    }

    private String extractClientid(String topic) {
        // $SYS/brokers/emqx@172.17.0.4/clients/hivemq_IPrintService_078d5b3d9ed24e0ebfbe26715ff0f537/connected
        return StrUtil.subBetween(topic, "clients/", "/");
    }

    public void publishDeviceEvent(EmqxClient emqxClient, EmqxClientStatus status) {
        EmqxClientEvent event = new EmqxClientEvent(emqxClient, status);
        log.info(" EVENT: {}", event);
        eventPublisher.publishEvent(event);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.initMqttClient();
        this.connectAndSubscribe();
    }
}