package com.jbm.cluster.api.model.api;

import lombok.Data;

/**
 * API 资源受控摘要，面向控制台展示和治理判断。
 */
@Data
public class ApiControlSummary {

    private String controlMode;

    private String visibility;

    private String authentication;

    private Long authorityCount = 0L;

    private Long apiKeyGrantCount = 0L;

    private Long rateLimitPolicyCount = 0L;

    private Long ipLimitPolicyCount = 0L;

    private Boolean externallyControlled;

    private Boolean internallyControlled;
}
