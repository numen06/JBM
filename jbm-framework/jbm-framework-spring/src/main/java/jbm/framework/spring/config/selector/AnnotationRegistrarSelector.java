package jbm.framework.spring.config.selector;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.*;

/**
 * @Created wesley.zhang
 * @Date 2022/4/24 2:05
 * @Description TODO
 */
public abstract class AnnotationRegistrarSelector implements ImportBeanDefinitionRegistrar, EnvironmentAware,
        BeanFactoryAware, ResourceLoaderAware {

    private static final Logger log = LoggerFactory.getLogger(AnnotationRegistrarSelector.class);

    private BeanFactory beanFactory;

    private ResourceLoader resourceLoader;

    private Environment environment;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }


    /**
     * 查找相应的内容
     *
     * @param importingClassMetadata
     * @param registry
     * @param annotation
     * @param classPathBeanDefinitionScanner
     * @return
     */
    private Set<BeanDefinitionHolder> findBeanDefinition(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry, Class annotation, ClassPathBeanScannerProvider classPathBeanDefinitionScanner) {
        Set<BeanDefinitionHolder> beanDefinitionHolders = new HashSet<>();
        AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(importingClassMetadata
                .getAnnotationAttributes(annotation.getCanonicalName()));
        if (annotationAttributes == null) {
            log.warn("No EnableEventBus Annotation.");
            return beanDefinitionHolders;
        }
        beanDefinitionHolders = classPathBeanDefinitionScanner.doScan(this.getPackages(annotation, importingClassMetadata));
        return beanDefinitionHolders;
    }

    /**
     * 通过注解中的targetPackage,targetPackage,targetPackageClasses来扫描
     *
     * @param annotation
     * @param metadata
     * @return
     */
    protected Set<String> getPackages(Class annotation, AnnotationMetadata metadata) {
        Set<String> packagesToScan = new LinkedHashSet<>();
        if (metadata.hasAnnotatedMethods("targetPackage")) {
            AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(annotation.getName()));
            packagesToScan = Sets.newHashSet(attributes.getString("targetPackage"));
        } else {
            AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(annotation.getName()));
            String[] basePackages = attributes.getStringArray("targetPackages");
            Class<?>[] basePackageClasses = attributes.getClassArray("targetPackageClasses");
            packagesToScan.addAll(Arrays.asList(basePackages));
            for (Class<?> basePackageClass : basePackageClasses) {
                packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
            }
            if (packagesToScan.isEmpty()) {
                String packageName = ClassUtils.getPackageName(metadata.getClassName());
                Assert.state(!ObjectUtil.isEmpty(packageName), StrUtil.format("@{} cannot be used with the default package", annotation));
                return Collections.singleton(packageName);
            }
        }
        return packagesToScan;
    }


    public abstract boolean isEnabled();

    public abstract void processBeanDefinition(Set<BeanDefinitionHolder> beanDefinitionHolders);

}
