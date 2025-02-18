package com.jbm.framework.boot.autoconfigure.retrofit;

import com.jbm.framework.boot.autoconfigure.retrofit.auth.AuthStrategy;
import org.springframework.context.ApplicationContext;

/**
 * @author wesley
 */
public class StrategyFactory extends AbstractStrategyFactory {

    public StrategyFactory(ApplicationContext applicationContext, PlatformsProperties platformsProperties) {
        super(applicationContext, platformsProperties);
    }

}