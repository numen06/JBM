package jbm.framework.boot.autoconfigure.emqx.hook.dto;

import lombok.Data;

/**
 * EMQX HTTP 认证响应 DTO（result: allow/deny/ignore）
 */
@Data
public class EmqxAuthResponse {
    private String result;
    private Boolean is_superuser;
    private String reason;
}
