package com.jbm.framework.boot.autoconfigure.retrofit;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ReflectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
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
@Slf4j
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
        strategies.computeIfAbsent(platformName, k -> new ArrayList<>());
        return strategies.get(platformName);
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
        for (Class<? extends BaseStrategy> baseStrategyClass : baseStrategyClassArray) {
            try {
                if (applicationContext.containsBean(baseStrategyClass.getSimpleName())) {
                    continue;
                }
                BaseStrategy baseStrategy = ReflectUtil.newInstance(baseStrategyClass);
                baseStrategy.setPlatform(platform);
                applicationContext.getAutowireCapableBeanFactory().initializeBean(baseStrategy, baseStrategyClass.getSimpleName());
                applicationContext.getAutowireCapableBeanFactory().autowireBean(baseStrategy);
                baseStrategyList.add(baseStrategy);
            }  catch (Exception e) {
                log.error("registerStrategy error", e);
            }
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
