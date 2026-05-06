package jbm.framework.boot.autoconfigure.emqx.hook.dto;

import lombok.Data;

/**
 * EMQX HTTP 认证请求 DTO（与 EMQX 官方钩子协议一致）
 */
@Data
public class EmqxAuthRequest {
    private String clientid;
    private String username;
    private String password;
    private String peerhost;
    private String protocol;
    private String proto_ver;
}
