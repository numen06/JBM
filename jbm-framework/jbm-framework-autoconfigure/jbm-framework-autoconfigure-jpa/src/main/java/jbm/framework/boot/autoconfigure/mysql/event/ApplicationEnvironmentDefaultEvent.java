package jbm.framework.boot.autoconfigure.mysql.event;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.Lists;
import jbm.framework.spring.config.ApplicationEnvironmentDefaultListener;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

/**
 * 默认配置文件注入
 *
 * @author wesley
 */
public class ApplicationEnvironmentDefaultEvent extends ApplicationEnvironmentDefaultListener {

    private static final String PROPERTIES = "classpath:configs/jpa.properties";

    @Override
    public List<String> loadPropertiesPath() {
        return Lists.newArrayList(PROPERTIES);
    }

    @Override
    public Map<String, Object> loadMapConfigs() {
        return null;
    }

}
