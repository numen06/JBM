package com.jbm.framework.cloud.config.event;

import com.google.common.collect.Lists;
import jbm.framework.spring.config.ApplicationEnvironmentDefaultListener;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author: create by wesley
 * @date:2018/12/16
 */
public class ApplicationEnvironmentDefaultEvent extends ApplicationEnvironmentDefaultListener {
    @Override
    public List<String> loadPropertiesPath() {
        return Lists.newArrayList("cloud-config.yml");
    }

    @Override
    public Map<String, Object> loadMapConfigs() {
        return null;
    }
}
