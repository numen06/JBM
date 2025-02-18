package com.jbm.framework.boot.autoconfigure.retrofit;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import retrofit2.Retrofit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wesley
 */
public class AbstractStrategyFactory implements InitializingBean {

    protected final Map<String, List<Strategy>> strategies = new ConcurrentHashMap<>();

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

    public List<Strategy> getStrategys(String platformName) {
        List<Strategy> st = strategies.get(platformName);
        if (st == null) {
            return new ArrayList<>();
        }
        return st;
    }

    public <T extends Strategy> T getStrategy(PlatformsProperties.Platform platform, Class<T> strategyClass) {
        List<Strategy> strategyList = this.getStrategys(platform.getName());
        for (Strategy strategy : strategyList)
            if (strategyClass.isInstance(strategy)) {
                T t =  (T) strategy;
                applicationContext.getAutowireCapableBeanFactory().autowireBean(t);
//                Retrofit retrofit=   applicationContext.getBean(Retrofit.class);
//                t.setRetrofit(retrofit);
                return t;
            }
        return null;
    }


    public void registerStrategy(PlatformsProperties.Platform platform) {
        platform.getStrategys().forEach((strategyName) -> {
            Class<Strategy> strategyClass = ClassUtil.loadClass(strategyName);
            if (strategyClass == null) {
                throw new IllegalArgumentException("策略类不存在");
            }
            Strategy strategy = ReflectUtil.newInstance(strategyClass);
            this.registerStrategy(platform, strategy);
        });
    }

    public void registerStrategy(PlatformsProperties.Platform platform, Strategy strategy) {
//        strategy.setClient(okHttpClient);
//        strategy.setRetrofit(retrofit);
        strategy.setPlatform(platform);
        List<Strategy> strategyList = this.getStrategys(platform.getName());
        strategyList.add(strategy);
        strategies.putIfAbsent(platform.getName(), strategyList);
    }

    /**
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        platformsProperties.getPlatforms().values().forEach(this::registerStrategy);
    }
}
