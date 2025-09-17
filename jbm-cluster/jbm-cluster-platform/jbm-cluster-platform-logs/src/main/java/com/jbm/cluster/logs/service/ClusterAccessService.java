package com.jbm.cluster.logs.service;

import com.jbm.cluster.logs.form.ClusterAccessInfo;

/**
 * @author wesley
 */
public interface ClusterAccessService {
    void accumulate(int count);

    ClusterAccessInfo getClusterAccessInfo();
}
