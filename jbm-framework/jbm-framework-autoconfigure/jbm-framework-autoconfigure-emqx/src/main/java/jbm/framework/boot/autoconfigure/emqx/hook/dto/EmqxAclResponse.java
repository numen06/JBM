package jbm.framework.boot.autoconfigure.emqx.hook.dto;

import lombok.Data;

/**
 * EMQX HTTP ACL 响应 DTO（result: allow/deny/ignore）
 */
@Data
public class EmqxAclResponse {
    private String result;
    private String reason;
}
