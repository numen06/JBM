package jbm.framework.boot.autoconfigure.emqx.hook.dto;

import lombok.Data;

/**
 * EMQX HTTP ACL 请求 DTO（access: publish/subscribe）
 */
@Data
public class EmqxAclRequest {
    private String clientid;
    private String username;
    private String access;
    private String topic;
    private String ipaddr;
}
