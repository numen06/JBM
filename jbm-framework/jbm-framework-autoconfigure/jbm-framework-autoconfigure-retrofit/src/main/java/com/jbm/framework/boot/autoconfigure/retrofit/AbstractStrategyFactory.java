package com.jbm.framework.boot.autoconfigure.retrofit;

import cn.hutool.core.annotation.AnnotationUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import retrofit2.Invocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wesley
 */
public class AbstractStrategyFactory implements InitializingBean {

    protected final Map<String, List<BaseStrategy>> strategies = new ConcurrentHashMap<>();

    protected final ApplicationContext applicationContext;

    protected final PlatformsProperties platformsProperties;

//    @Autowired
//    private OkHttpClient okHttpClient;
//    @Autowired
//    private Retrofit retrofit;

    public AbstractStrategyFactory(ApplicationContext applicationContext, PlatformsProperties platformsProperties) {
        this.applicationContext = applicationContext;
        this.platformsProperties = platformsProperties;
    }


    private final Map<Class<?>, PlatformsProperties.Platform> CLASS_PLATFORM_CACHE = new ConcurrentHashMap<>();

    public PlatformsProperties.Platform getPlatform(Invocation invocation) {
        Class<?> declaringClass = invocation.method().getDeclaringClass();
        if (CLASS_PLATFORM_CACHE.containsKey(declaringClass)) {
            return CLASS_PLATFORM_CACHE.get(declaringClass);
        }
        ApiPlatform apiPlatform = AnnotationUtil.getAnnotation(declaringClass, ApiPlatform.class);
        if (apiPlatform == null) {
            return null;
        }
        String platformName = apiPlatform.name();
        PlatformsProperties.Platform platform = platformsProperties.getPlatforms().get(platformName);
        this.registerStrategy(platform, apiPlatform.strategys());
        CLASS_PLATFORM_CACHE.put(declaringClass, platform);
        return platform;
    }


    public List<BaseStrategy> getStrategys(String platformName) {
        List<BaseStrategy> st = strategies.get(platformName);
        if (st == null) {
            return new ArrayList<>();
        }
        return st;
    }

    public <T extends BaseStrategy> T getStrategy(Invocation invocation, Class<T> strategyClass) {
        PlatformsProperties.Platform platform = this.getPlatform(invocation);
        if (platform == null) {
            return null;
        }
        return this.getStrategy(platform, strategyClass);
    }


    public <T extends BaseStrategy> T getStrategy(PlatformsProperties.Platform platform, Class<T> strategyClass) {
        List<BaseStrategy> baseStrategyList = this.getStrategys(platform.getName());
        for (BaseStrategy baseStrategy : baseStrategyList) {
            if (strategyClass.isInstance(baseStrategy)) {
                return (T) baseStrategy;
            }
        }
        return null;
    }

    public void registerStrategy(PlatformsProperties.Platform platform, Class<? extends BaseStrategy>[] baseStrategyClassArray) {
//        strategy.setClient(okHttpClient);
//        strategy.setRetrofit(retrofit);
        List<BaseStrategy> baseStrategyList = this.getStrategys(platform.getName());
        if (baseStrategyList == null) {
            baseStrategyList = new ArrayList<>();
        }
        strategies.putIfAbsent(platform.getName(), baseStrategyList);
        for (Class<? extends BaseStrategy> baseStrategyClass : baseStrategyClassArray) {
            BaseStrategy baseStrategy = applicationContext.getAutowireCapableBeanFactory().createBean(baseStrategyClass);
            baseStrategy.setPlatform(platform);
            baseStrategyList.add(baseStrategy);
        }

    }

    /**
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
//        platformsProperties.getPlatforms().values().forEach(this::registerStrategy);
    }

}
