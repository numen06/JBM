package com.jbm.cluster.api.model.api;

import lombok.Data;

/**
 * API 资源治理计数。
 */
@Data
public class ApiControlCount {

    private Long apiId;

    private Long count;
}
