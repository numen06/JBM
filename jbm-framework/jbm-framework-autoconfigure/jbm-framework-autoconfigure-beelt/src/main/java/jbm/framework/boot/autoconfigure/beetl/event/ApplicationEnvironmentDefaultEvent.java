package jbm.framework.boot.autoconfigure.beetl.event;

import com.google.common.collect.Lists;
import jbm.framework.spring.config.ApplicationEnvironmentDefaultListener;

import java.util.List;
import java.util.Properties;

public class ApplicationEnvironmentDefaultEvent extends ApplicationEnvironmentDefaultListener {


    @Override
    public List<String> loadPropertiesPath() {
        return Lists.newArrayList("beetl");
    }

    @Override
    public Properties initProperties() {
        return null;
    }
}
