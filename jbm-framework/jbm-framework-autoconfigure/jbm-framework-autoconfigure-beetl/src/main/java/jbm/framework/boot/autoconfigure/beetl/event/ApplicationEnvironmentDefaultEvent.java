package jbm.framework.boot.autoconfigure.beetl.event;

import com.google.common.collect.Lists;
import jbm.framework.spring.config.ApplicationEnvironmentDefaultListener;

import java.util.List;
import java.util.Map;

public class ApplicationEnvironmentDefaultEvent extends ApplicationEnvironmentDefaultListener {


    @Override
    public List<String> loadPropertiesPath() {
        return Lists.newArrayList("beetl");
    }

    @Override
    public Map<String, Object> loadMapConfigs() {
        return null;
    }


}
