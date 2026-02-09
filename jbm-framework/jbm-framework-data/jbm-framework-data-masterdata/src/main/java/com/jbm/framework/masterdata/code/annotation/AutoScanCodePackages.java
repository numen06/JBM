package com.jbm.framework.masterdata.code.annotation;

import cn.hutool.core.util.StrUtil;
import com.jbm.framework.masterdata.code.GenerateHelper;
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

    public static AutoScanCodePackages get(BeanFactory beanFactory) {
        // Currently we only store a single base package, but we return a list to
        // allow this to change in the future if needed
        try {
            return beanFactory.getBean(BEAN, AutoScanCodePackages.class);
        } catch (NoSuchBeanDefinitionException ex) {
            return NONE;
        }
    }

    public static void gnerate(BeanDefinitionRegistry registry, Map<String, Object> attributes,
                               Collection<String> packageNames) {
        Assert.notNull(registry, "Registry must not be null");
        Assert.notNull(packageNames, "PackageNames must not be null");
        for (String pk : packageNames) {
            GenerateHelper.scanGnerate(pk, attributes);
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
            Map<String, Object> raw = metadata.getAnnotationAttributes(EnableCodeAutoGeneate.class.getName());
            Map<String, Object> effective = new LinkedHashMap<>(raw != null ? raw : Collections.emptyMap());
            expandMapperConfig(effective, raw);
            expandServiceConfig(effective, raw);
            expandControllerConfig(effective, raw);
            expandBusinessConfig(effective, raw);
            effective.remove("mapper");
            effective.remove("service");
            effective.remove("controller");
            effective.remove("business");
            gnerate(registry, effective, getPackagesToScan(metadata));
        }

        private static void putIfBlank(Map<String, Object> effective, String key, String value) {
            if (value == null) return;
            Object cur = effective.get(key);
            if (cur == null || (cur instanceof String && !StringUtils.hasText((String) cur))) {
                effective.put(key, value);
            }
        }

        private static void expandMapperConfig(Map<String, Object> effective, Map<String, Object> raw) {
            Object obj = raw != null ? raw.get("mapper") : null;
            if (!(obj instanceof AnnotationAttributes)) return;
            AnnotationAttributes a = (AnnotationAttributes) obj;
            String module = a.getString("module");
            String packageBase = a.getString("packageBase");
            if (StringUtils.hasText(module)) {
                putIfBlank(effective, "daoModule", module);
                putIfBlank(effective, "mapperModule", module);
                putIfBlank(effective, "mapperXmlModule", module);
            }
            if (StringUtils.hasText(packageBase)) {
                putIfBlank(effective, "mapperPackage", packageBase + ".mapper");
            }
        }

        private static void expandServiceConfig(Map<String, Object> effective, Map<String, Object> raw) {
            Object obj = raw != null ? raw.get("service") : null;
            if (!(obj instanceof AnnotationAttributes)) return;
            AnnotationAttributes a = (AnnotationAttributes) obj;
            String module = a.getString("module");
            String packageBase = a.getString("packageBase");
            if (StringUtils.hasText(module)) {
                putIfBlank(effective, "serviceModule", module);
                putIfBlank(effective, "serviceImplModule", module);
            }
            if (StringUtils.hasText(packageBase)) {
                putIfBlank(effective, "servicePackage", packageBase + ".service");
            }
        }

        private static void expandControllerConfig(Map<String, Object> effective, Map<String, Object> raw) {
            Object obj = raw != null ? raw.get("controller") : null;
            if (!(obj instanceof AnnotationAttributes)) return;
            AnnotationAttributes a = (AnnotationAttributes) obj;
            String module = a.getString("module");
            String packageBase = a.getString("packageBase");
            if (StringUtils.hasText(module)) {
                putIfBlank(effective, "controllerModule", module);
            }
            if (StringUtils.hasText(packageBase)) {
                putIfBlank(effective, "controllerPackage", packageBase + ".controller");
            }
        }

        private static void expandBusinessConfig(Map<String, Object> effective, Map<String, Object> raw) {
            Object obj = raw != null ? raw.get("business") : null;
            if (!(obj instanceof AnnotationAttributes)) return;
            AnnotationAttributes a = (AnnotationAttributes) obj;
            String module = a.getString("module");
            String packageBase = a.getString("packageBase");
            if (StringUtils.hasText(module)) {
                putIfBlank(effective, "businessModule", module);
                putIfBlank(effective, "businessImplModule", module);
            }
            if (StringUtils.hasText(packageBase)) {
                putIfBlank(effective, "businessPackage", packageBase + ".business");
            }
        }

//        private String getPackagesToGeneate(AnnotationMetadata metadata) {
//            AnnotationAttributes attributes = AnnotationAttributes.fromMap(
//                    metadata.getAnnotationAttributes(EnableCodeAutoGeneate.class.getName()));
//            return attributes.getString("targetPackage");
//        }

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
                Assert.state(!StrUtil.isEmpty(packageName),
                        "@EntityScan cannot be used with the default package");
                return Collections.singleton(packageName);
            }
            return packagesToScan;
        }

    }


}
