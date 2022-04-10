package com.jbm.cluster.node.configuration.cluster;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jbm.cluster.common.annotation.JbmClusterEvent;
import com.jbm.cluster.common.bus.JbmClusterEventBean;
import com.jbm.cluster.common.constants.QueueConstants;
import com.jbm.cluster.node.configuration.annotation.EnableJbmCluster;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@Slf4j
public class JbmClusterEventRegistry implements ImportBeanDefinitionRegistrar, BeanDefinitionRegistryPostProcessor, InitializingBean {
    @Getter
    private static Set<String> registryPackages = new LinkedHashSet<>();
    @Getter
    private List<JbmClusterEventBean> registryBeanList = Lists.newArrayList();


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
//        if (ObjectUtil.isEmpty(this.amqpTemplate)) {
//            log.warn("JBM节点无法连接MQ");
//            return;
//        }
//        ThreadUtil.execAsync(new Runnable() {
//            @Override
//            public void run() {
//                amqpTemplate.convertAndSend(QueueConstants.QUEUE_SCAN_EVENT, registryBeanList);
//            }
//        });
    }

    /**
     * 扫描对应类
     *
     * @param registry
     * @throws BeansException
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        //定义一个扫描器,设置为true只在spring容器中搜索
        ClassPathScanningCandidateComponentProvider beanScanner = new ClassPathScanningCandidateComponentProvider(true);
        AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(RestController.class);
        AnnotationTypeFilter annotationTypeFilter2 = new AnnotationTypeFilter(RequestMapping.class);
        beanScanner.addIncludeFilter(annotationTypeFilter);
        beanScanner.addIncludeFilter(annotationTypeFilter2);
        for (String basePackage : registryPackages) {
            Set<BeanDefinition> beanDefinitions = beanScanner.findCandidateComponents(basePackage);
            for (BeanDefinition beanDefinition : beanDefinitions) {
                String beanName = beanDefinition.getBeanClassName();
                Class<?> clazz = ClassUtil.loadClass(beanDefinition.getBeanClassName());
                StandardAnnotationMetadata beanAnnotationMetadata = new StandardAnnotationMetadata(clazz, true);
                Set<MethodMetadata> methodMetadatas = beanAnnotationMetadata.getAnnotatedMethods(JbmClusterEvent.class.getName());
                for (MethodMetadata methodMetadata : methodMetadatas) {
                    JbmClusterEventBean jbmClusterEventBean = new JbmClusterEventBean();
                    if (beanAnnotationMetadata.hasAnnotation(PostMapping.class.getName())) {
                        jbmClusterEventBean.setMethodType(HttpMethod.POST.toString());
                        jbmClusterEventBean.setJobName(methodMetadata.getMethodName());
                    }
                    if (beanAnnotationMetadata.hasAnnotation(GetMapping.class.getName())) {
                        jbmClusterEventBean.setMethodType(HttpMethod.GET.toString());
                        jbmClusterEventBean.setJobName(methodMetadata.getMethodName());
                    }
                    log.info("扫描到方法:{}的{}方法", beanName, methodMetadata.getMethodName());
                    Map<String, Object> attributes = methodMetadata.getAnnotationAttributes(JbmClusterEvent.class.getName());
                    this.buildRegistryBean(jbmClusterEventBean, attributes);
                }
            }
        }
    }

    public void buildRegistryBean(Map<String, Object> attributes) {
        this.buildRegistryBean(new JbmClusterEventBean(), attributes);
    }

    public void buildRegistryBean(JbmClusterEventBean jbmClusterEventBean, Map<String, Object> attributes) {
        BeanUtil.fillBeanWithMap(attributes, jbmClusterEventBean, true);
        registryBeanList.add(jbmClusterEventBean);
    }


    /**
     * 启用注册方法
     *
     * @param metadata
     * @param registry
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        registryPackages = this.getPackages(metadata);
//        this.postProcessBeanDefinitionRegistry(registry);
    }

    private Set<String> getPackages(AnnotationMetadata metadata) {
        Set<String> packagesToScan = new LinkedHashSet<>();
        if (metadata.hasAnnotatedMethods("targetPackage")) {
            AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(EnableJbmCluster.class.getName()));
            packagesToScan = Sets.newHashSet(attributes.getString("targetPackage"));
        } else {
            AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(EnableJbmCluster.class.getName()));
            String[] basePackages = attributes.getStringArray("targetPackages");
            Class<?>[] basePackageClasses = attributes.getClassArray("targetPackageClasses");
            packagesToScan.addAll(Arrays.asList(basePackages));
            for (Class<?> basePackageClass : basePackageClasses) {
                packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
            }
            if (packagesToScan.isEmpty()) {
                String packageName = ClassUtils.getPackageName(metadata.getClassName());
                Assert.state(!StringUtils.isEmpty(packageName), "@EntityScan cannot be used with the default package");
                return Collections.singleton(packageName);
            }
        }
        return packagesToScan;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
//        log.info("{}", amqpTemplate);
//        this.postProcessBeanDefinitionRegistry(null);
    }
}
