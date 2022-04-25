package jbm.framework.spring.config.selector;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.TypeFilter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @Created wesley.zhang
 * @Date 2022/4/24 1:49
 * @Description 注解扫描器
 */
public class ClassPathBeanScannerProvider extends ClassPathBeanDefinitionScanner {

    public ClassPathBeanScannerProvider(BeanDefinitionRegistry registry, boolean useDefaultFilters) {
        super(registry, useDefaultFilters);
    }


    public Set<BeanDefinitionHolder> doScan(Set<String> basePackages) {
        Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
            for (BeanDefinition candidate : candidates) {
                BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, candidate.getBeanClassName());
                beanDefinitions.add(definitionHolder);
            }
        }
        return beanDefinitions;
    }
}
