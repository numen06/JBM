package com.jbm.cluster.common.satoken.env;

import jbm.framework.spring.config.AbstractEnvironmentPostProcessor;
import jbm.framework.spring.config.annotation.ProloadConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @Created wesley.zhang
 * @Date 2022/4/25 0:20
 * @Description TODO
 */
@ProloadConfig(properties = "SaTokenEnv.properties")
public class SaTokenEnvProcessor extends AbstractEnvironmentPostProcessor {
    @Override
    public void postProcessEnvironmentAfter(ConfigurableEnvironment environment, SpringApplication application) {

    }
}
