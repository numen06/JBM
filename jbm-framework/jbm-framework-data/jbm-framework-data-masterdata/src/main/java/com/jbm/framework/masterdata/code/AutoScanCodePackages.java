package com.jbm.framework.masterdata.code;

import com.jbm.framework.masterdata.annotation.MasterData;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author: create by wesley
 * @date:2019/4/28
 */
public class AutoScanCodePackages {

    private static final String BEAN = AutoScanCodePackages.class.getName();

    private static final AutoScanCodePackages NONE = new AutoScanCodePackages();

    private final List<String> packageNames;

    AutoScanCodePackages(String... packageNames) {
        List<String> packages = new ArrayList<>();
        for (String name : packageNames) {
            if (StringUtils.hasText(name)) {
                packages.add(name);
            }
        }
        this.packageNames = Collections.unmodifiableList(packages);
    }

    public List<String> getPackageNames() {
        return this.packageNames;
    }

    public static AutoScanCodePackages get(BeanFactory beanFactory) {
        // Currently we only store a single base package, but we return a list to
        // allow this to change in the future if needed
        try {
            return beanFactory.getBean(BEAN, AutoScanCodePackages.class);
        } catch (NoSuchBeanDefinitionException ex) {
            return NONE;
        }
    }

    public static void gnerate(BeanDefinitionRegistry registry, String targetPackage,
                               Collection<String> packageNames) {
        Assert.notNull(registry, "Registry must not be null");
        Assert.notNull(packageNames, "PackageNames must not be null");
        for (String pk : packageNames) {
            GenerateMasterData.scanGnerate(pk, targetPackage);
        }
    }

    private static String[] addPackageNames(
            ConstructorArgumentValues constructorArguments,
            Collection<String> packageNames) {
        String[] existing = (String[]) constructorArguments
                .getIndexedArgumentValue(0, String[].class).getValue();
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(Arrays.asList(existing));
        merged.addAll(packageNames);
        return StringUtils.toStringArray(merged);
    }

    static class CodeRegistrar implements ImportBeanDefinitionRegistrar {

        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
            gnerate(registry, getPackagesToGeneate(metadata), getPackagesToScan(metadata));

        }

        private String getPackagesToGeneate(AnnotationMetadata metadata) {
            AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                    metadata.getAnnotationAttributes(EnableCodeAutoGeneate.class.getName()));
            return attributes.getString("targetPackage");
        }

        private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
            AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                    metadata.getAnnotationAttributes(EnableCodeAutoGeneate.class.getName()));
            String[] basePackages = attributes.getStringArray("entityPackages");
            Class<?>[] basePackageClasses = attributes
                    .getClassArray("entityPackageClasses");
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
