package jbm.framework.boot.autoconfigure.mqtt.registrar;

import jbm.framework.boot.autoconfigure.mqtt.annotation.MqttMapper;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author wesley
 */
public class MqttScannerRegistrar implements ImportBeanDefinitionRegistrar {


    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        // 获取SpringBootApplication自定义注解CkScan值
        AnnotationAttributes attrs = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(EnableMqttMapperScan.class.getName()));

        if (attrs != null) {
            ArrayList<String> basePackages = new ArrayList<>();
            basePackages.addAll(Arrays.stream(attrs.getStringArray("value")).filter(StringUtils::hasText).collect(Collectors.toList()));
            basePackages.addAll(Arrays.stream(attrs.getStringArray("basePackages")).filter(StringUtils::hasText).collect(Collectors.toList()));
            basePackages.addAll(Arrays.stream(attrs.getClassArray("basePackageClasses")).map(ClassUtils::getPackageName).collect(Collectors.toList()));


            //将接口转换为BeanDefinition对象放入spring中
            //CkClassPathScanner为自定义扫描类
            MqttClassPathScanner classPathScanner = new MqttClassPathScanner(beanDefinitionRegistry);
            classPathScanner.doScan(StringUtils.collectionToCommaDelimitedString(basePackages));
        }

    }
}


