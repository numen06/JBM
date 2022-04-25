package jbm.framework.spring.config;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.util.ClassUtils;
import com.jbm.util.PropertiesUtils;
import jbm.framework.spring.config.annotation.ProloadConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author wesley.zhang
 * @description 预设参数类
 */
public abstract class AbstractEnvironmentPostProcessor implements EnvironmentPostProcessor {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final static String APP_NAME_KEY = "spring.application.name";

    /**
     * 预加载设置目录
     */
    public final static String RROLOAD_CONFIG_DIR = "classpath:configs/";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            if (!AnnotationUtil.hasAnnotation(getClass(), ProloadConfig.class))
                return;
            ProloadConfig proloadConfig = AnnotationUtil.getAnnotation(getClass(), ProloadConfig.class);
            this.proloadProperties(environment, proloadConfig.properties());
        } finally {
            this.postProcessEnvironmentAfter(environment, application);
        }
    }

    public abstract void postProcessEnvironmentAfter(ConfigurableEnvironment environment, SpringApplication application);

    /**
     * 预设值
     */
    public void proloadProperties(ConfigurableEnvironment environment, String propertiesName) {
        final String propertySourceName = ClassUtils.getShortNameAsProperty(getClass()) + "Properties";
        if (environment.containsProperty(propertySourceName)) {
            if (Boolean.FALSE.toString()
                    .equalsIgnoreCase(environment.getProperty(propertySourceName))) {
                return;
            }
        }
        Map<String, Object> defaults = this.loadProperties(environment, propertiesName);
        if (CollectionUtils.isEmpty(defaults)) {
            return;
        }
        this.addOrReplace(environment.getPropertySources(), defaults, propertySourceName, true);
        log.info("jbm proload properties:{}", propertiesName);
    }


    public Map<String, Object> loadProperties(ConfigurableEnvironment environment, String propertiesName) {
        Map<String, Object> datamap = new HashMap<>();
        try {
            Properties properties = PropertiesUtils.loadClassPath(RROLOAD_CONFIG_DIR + propertiesName);
            for (String key : properties.stringPropertyNames()) {
                String val = properties.getProperty(key);
                String[] arr = StrUtil.subBetweenAll(val, "${", "}");
                if (ArrayUtil.isNotEmpty(arr)) {
                    for (int i = 0; i < arr.length; i++) {
                        //检索的KEY
                        String kk = arr[i];
                        //替换的结果
                        String pp = environment.getProperty(kk);
                        val = StrUtil.replace(val, "${" + kk + "}", pp);
                    }
                }
                datamap.put(key, val);
            }
        } catch (Exception e) {
            log.error("预加载配置文件错误,请检查[{}]目录", RROLOAD_CONFIG_DIR);
        }
        return datamap;
    }


    /**
     * 获取应用名称
     *
     * @param environment
     * @return
     */
    public String getApplicationName(ConfigurableEnvironment environment) {
        return environment.getProperty(APP_NAME_KEY);
    }

    /***
     *
     * 插入配置到环境变量中
     * @param propertySources 数据源
     * @param map 插入对此
     * @param propertySourceName 配置名称
     * @param first 是否作为前置，true代表可以被覆盖
     */
    public void addOrReplace(MutablePropertySources propertySources, Map<String, Object> map,
                             String propertySourceName, boolean first) {
        MapPropertySource target = null;
        if (propertySources.contains(propertySourceName)) {
            PropertySource<?> source = propertySources.get(propertySourceName);
            if (source instanceof MapPropertySource) {
                target = (MapPropertySource) source;
                for (String key : map.keySet()) {
                    if (!target.containsProperty(key)) {
                        target.getSource().put(key, map.get(key));
                    }
                }
            }
        }
        if (target == null) {
            target = new MapPropertySource(propertySourceName, map);
        }
        if (!propertySources.contains(propertySourceName)) {
            if (first) {
                propertySources.addFirst(target);
            } else {
                propertySources.addLast(target);
            }
        }
    }


}
