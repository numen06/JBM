package com.jbm.framework.cloud.registry.event;

import com.google.common.collect.Lists;
import jbm.framework.spring.config.ApplicationEnvironmentDefaultListener;

import java.util.List;
import java.util.Map;

/**
 * @author: create by wesley
 * @date:2018/12/16
 */
public class ApplicationEnvironmentDefaultEvent extends ApplicationEnvironmentDefaultListener {
    @Override
    public List<String> loadPropertiesPath() {
        return Lists.newArrayList("cloud-registry.yml");
    }

    @Override
    public Map<String, Object> loadMapConfigs() {
        return null;
    }
}
