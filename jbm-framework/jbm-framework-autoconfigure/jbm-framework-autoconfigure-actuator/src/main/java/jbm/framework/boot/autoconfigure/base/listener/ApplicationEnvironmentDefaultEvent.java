package jbm.framework.boot.autoconfigure.base.listener;

import com.google.common.collect.Lists;
import jbm.framework.spring.config.ApplicationEnvironmentDefaultListener;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;


/**
 * 默认配置文件注入
 *
 * @author wesley
 */
@Slf4j
public class ApplicationEnvironmentDefaultEvent extends ApplicationEnvironmentDefaultListener {

    private static final String PROPERTIES = "classpath:configs/metrics.yml";

    @Override
    public List<String> loadPropertiesPath() {
        return Lists.newArrayList(PROPERTIES);
    }

    @Override
    public Map<String, Object> loadMapConfigs() {
        return null;
    }


}
