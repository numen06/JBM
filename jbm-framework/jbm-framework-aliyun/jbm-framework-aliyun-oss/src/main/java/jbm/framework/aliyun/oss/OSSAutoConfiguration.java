package jbm.framework.aliyun.oss;

import cn.hutool.core.map.MapUtil;
import com.google.common.collect.Maps;
import jbm.framework.aliyun.oss.config.OSSClientProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;


/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-09-12 02:20
 **/
@Configuration
@ConditionalOnProperty(prefix = OSSClientProperties.PREFIX)
@Slf4j
public class OSSAutoConfiguration implements ImportBeanDefinitionRegistrar, EnvironmentAware {


    private Map<String, OSSClientProperties> propertiesMap = Maps.newHashMap();

    @Override

    public void setEnvironment(Environment environment) {
        this.readProperties(environment);
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        for (String clientName : propertiesMap.keySet()) {
            OSSClientProperties ossClientProperties = propertiesMap.get(clientName);
            log.info("load oss client: {}", clientName);
            registry.registerBeanDefinition(clientName, this.ossClient(ossClientProperties));
        }
    }

    private void readProperties(Environment environment) {
        Binder binder = Binder.get(environment);
        Map<String, OSSClientProperties> propertiesMap = binder
                .bind(OSSClientProperties.PREFIX, Bindable.mapOf(String.class, OSSClientProperties.class)).get();
        if (MapUtil.isNotEmpty(propertiesMap))
            this.propertiesMap.putAll(propertiesMap);
    }

    private BeanDefinition ossClient(OSSClientProperties ossClientProperties) {

        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(OSSClientProperties.class);
        // 创建DynamicDataSource
        beanDefinitionBuilder.addConstructorArgValue(ossClientProperties.getEndpoint());
        beanDefinitionBuilder.addConstructorArgValue(ossClientProperties.getAccessKeyId());
        beanDefinitionBuilder.addConstructorArgValue(ossClientProperties.getAccessKeySecret());
        BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();

        return beanDefinition;
    }
}
