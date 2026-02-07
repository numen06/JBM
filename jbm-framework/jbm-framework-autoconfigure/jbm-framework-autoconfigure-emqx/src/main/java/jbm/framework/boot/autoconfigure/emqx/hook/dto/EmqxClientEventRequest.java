package jbm.framework.boot.autoconfigure.emqx.hook.dto;

import lombok.Data;

/**
 * EMQX 客户端事件请求 DTO（action: client.connected / client.disconnected）
 */
@Data
public class EmqxClientEventRequest {
    private String action;
    private String clientid;
    private String username;
    private String peerhost;
    private Integer peerport;
    private String protocol;
    private String proto_ver;
    private Long connected_at;
    private String reason;
}
