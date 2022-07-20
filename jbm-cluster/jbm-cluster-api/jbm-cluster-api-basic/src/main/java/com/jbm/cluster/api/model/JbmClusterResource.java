package com.jbm.cluster.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author wesley
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JbmClusterResource implements Serializable {

    /**
     * 服务ID
     */
    private String serviceId;
}
