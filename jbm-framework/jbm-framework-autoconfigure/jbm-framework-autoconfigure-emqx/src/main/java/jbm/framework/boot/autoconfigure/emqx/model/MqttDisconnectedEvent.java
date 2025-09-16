package jbm.framework.boot.autoconfigure.emqx.model;

import lombok.Data;

/**
 * @author wesley
 */
@Data
public class MqttDisconnectedEvent {
    private String eventType;
    private String clientId;
    private String username;
    private String reason;
    private Long disconnectedAt;
    private String node;
}