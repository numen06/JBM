package jbm.framework.boot.autoconfigure.mybatis.event;

import com.google.common.collect.Lists;
import jbm.framework.spring.config.ApplicationEnvironmentDefaultListener;

import java.util.List;
import java.util.Properties;

/**
 * 默认配置文件注入
 *
 * @author wesley
 */
public class ApplicationEnvironmentDefaultEvent extends ApplicationEnvironmentDefaultListener {

    private static final String PROPERTIES = "classpath:configs/mybatis-plus.properties";


    @Override
    public List<String> loadPropertiesPath() {
        return Lists.newArrayList(PROPERTIES);
    }

    @Override
    public Properties initProperties() {
        return null;
    }
}
