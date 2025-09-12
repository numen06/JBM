package jbm.framework.boot.autoconfigure.emqx.model;


import lombok.Data;

@Data
public class MqttConnectedEvent {
    private String eventType;
    private String clientId;
    private String username;
    private String ip;
    private Long connectedAt;
    private String node;
}