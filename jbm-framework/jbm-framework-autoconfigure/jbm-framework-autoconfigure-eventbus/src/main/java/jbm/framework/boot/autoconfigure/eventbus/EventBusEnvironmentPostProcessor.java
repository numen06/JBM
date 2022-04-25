package jbm.framework.boot.autoconfigure.eventbus;

import jbm.framework.spring.config.AbstractEnvironmentPostProcessor;
import jbm.framework.spring.config.annotation.ProloadConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author wesley
 * @create 2021/7/19 5:29 下午
 * @description 注册binder的binding配置
 */
@ProloadConfig(properties = "eventbus.properties")
public class EventBusEnvironmentPostProcessor extends AbstractEnvironmentPostProcessor {


    @Override
    public void postProcessEnvironmentAfter(ConfigurableEnvironment environment, SpringApplication application) {

    }

}
