package com.jbm.autoconfig.dic;

import com.jbm.autoconfig.dic.annotation.EnableJbmDictionary;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.*;

public class EnumScanPackages {

    private static final String BEAN = EnumScanPackages.class.getName();
    private static final EnumScanPackages NONE = new EnumScanPackages(new String[0]);
    private final List<String> packageNames;

    EnumScanPackages(String... packageNames) {
        List<String> packages = new ArrayList();
        String[] var3 = packageNames;
        int var4 = packageNames.length;

        for (int var5 = 0; var5 < var4; ++var5) {
            String name = var3[var5];
            if (StringUtils.hasText(name)) {
                packages.add(name);
            }
        }

        this.packageNames = Collections.unmodifiableList(packages);
    }

    public static EnumScanPackages get(BeanFactory beanFactory) {
        try {
            return (EnumScanPackages) beanFactory.getBean(BEAN, EnumScanPackages.class);
        } catch (NoSuchBeanDefinitionException var2) {
            return NONE;
        }
    }

    public static void register(BeanDefinitionRegistry registry, String... packageNames) {
        Assert.notNull(registry, "Registry must not be null");
        Assert.notNull(packageNames, "PackageNames must not be null");
        register(registry, (Collection) Arrays.asList(packageNames));
    }

    public static void register(BeanDefinitionRegistry registry, Collection<String> packageNames) {
        Assert.notNull(registry, "Registry must not be null");
        Assert.notNull(packageNames, "PackageNames must not be null");
        if (registry.containsBeanDefinition(BEAN)) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(BEAN);
            ConstructorArgumentValues constructorArguments = beanDefinition.getConstructorArgumentValues();
            constructorArguments.addIndexedArgumentValue(0, addPackageNames(constructorArguments, packageNames));
        } else {
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(EnumScanPackages.class);
            beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, StringUtils.toStringArray(packageNames));
            beanDefinition.setRole(2);
            registry.registerBeanDefinition(BEAN, beanDefinition);
        }

    }

    private static String[] addPackageNames(ConstructorArgumentValues constructorArguments, Collection<String> packageNames) {
        String[] existing = (String[]) ((String[]) constructorArguments.getIndexedArgumentValue(0, String[].class).getValue());
        Set<String> merged = new LinkedHashSet();
        merged.addAll(Arrays.asList(existing));
        merged.addAll(packageNames);
        return StringUtils.toStringArray(merged);
    }

    public List<String> getPackageNames() {
        return this.packageNames;
    }

    /**
     * {@link ImportBeanDefinitionRegistrar} to store the base package from the importing
     * configuration.
     */
    public static class Registrar implements ImportBeanDefinitionRegistrar {

        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata,
                                            BeanDefinitionRegistry registry) {
            register(registry, getPackagesToScan(metadata));
        }

        private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
            AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                    metadata.getAnnotationAttributes(EnableJbmDictionary.class.getName()));
            String[] basePackages = attributes.getStringArray("basePackages");
            Class<?>[] basePackageClasses = attributes
                    .getClassArray("basePackageClasses");
            Set<String> packagesToScan = new LinkedHashSet<>();
            packagesToScan.addAll(Arrays.asList(basePackages));
            for (Class<?> basePackageClass : basePackageClasses) {
                packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
            }
            if (packagesToScan.isEmpty()) {
                String packageName = ClassUtils.getPackageName(metadata.getClassName());
                Assert.state(!StringUtils.isEmpty(packageName),
                        "@EntityScan cannot be used with the default package");
                return Collections.singleton(packageName);
            }
            return packagesToScan;
        }

    }

}
