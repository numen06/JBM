package jbm.framework.boot.autoconfigure.eventbus.autoconfigure;

import jbm.framework.spring.config.selector.AnnotationRegistrarSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Set;

/**
 * @author wesley
 * @create 2021/7/20 10:43 上午
 * @description 扫描所有的事件相关类并缓存起来
 */
public class EventPubSubSelector extends AnnotationRegistrarSelector {

    private static final Logger log = LoggerFactory.getLogger(EventPubSubSelector.class);


    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void processBeanDefinition(Set<BeanDefinitionHolder> beanDefinitionHolders) {

    }
}
