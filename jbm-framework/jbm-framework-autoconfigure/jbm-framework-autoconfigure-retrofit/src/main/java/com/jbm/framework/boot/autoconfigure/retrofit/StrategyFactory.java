package com.jbm.framework.boot.autoconfigure.retrofit;

import org.springframework.context.ApplicationContext;

/**
 * @author wesley
 */
public class StrategyFactory extends AbstractStrategyFactory {

    public StrategyFactory(ApplicationContext applicationContext, PlatformsProperties platformsProperties) {
        super(applicationContext, platformsProperties);
    }

}