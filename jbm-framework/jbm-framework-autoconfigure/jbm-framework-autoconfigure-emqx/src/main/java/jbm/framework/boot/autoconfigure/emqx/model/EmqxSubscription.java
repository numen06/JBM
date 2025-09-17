package jbm.framework.boot.autoconfigure.emqx.model;


import lombok.Data;

/**
 * {
 * "node" : "emqx@10.0.2.2",
 * "nl" : 0,
 * "durable" : false,
 * "topic" : "tpm-iot-protocol/platform/souther001/modeChange",
 * "clientid" : "tpm-iot-protocol/platform/souther001",
 * "qos" : 1,
 * "rap" : 0,
 * "rh" : 0
 * }
 *
 * @author wesley
 */
@Data
public class EmqxSubscription {
    private String node;
    private String clientId;
    private String topic;
    private Integer qos;
    private Integer durable;
}