package com.jbm.framework.masterdata.code.transaction;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import javax.annotation.Resource;

@Aspect
@Configuration
@Slf4j
public class TransactionAdviceConfig implements ImportBeanDefinitionRegistrar {

    private String targetPackages;

    @Resource
    private PlatformTransactionManager transactionManager;

    /**
     * 全局事务切面配置
     * 只对写操作方法（add*, save*, delete*, update*, exec*, set*）添加事务
     * 查询方法（get*, query*, find*, list*, count*, is*）不添加事务，避免影响写操作
     */
    @Bean
    public TransactionInterceptor txAdvice() {
        log.info("JBM开始切面事务");

        DefaultTransactionAttribute txAttr_REQUIRED = new DefaultTransactionAttribute();
        txAttr_REQUIRED.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
        // 只对写操作方法添加事务
        source.addTransactionalMethod("add*", txAttr_REQUIRED);
        source.addTransactionalMethod("save*", txAttr_REQUIRED);
        source.addTransactionalMethod("delete*", txAttr_REQUIRED);
        source.addTransactionalMethod("update*", txAttr_REQUIRED);
        source.addTransactionalMethod("exec*", txAttr_REQUIRED);
        source.addTransactionalMethod("set*", txAttr_REQUIRED);
        // 查询方法不添加事务，已移除以下配置：
        // get*, query*, find*, list*, count*, is* 等方法不再自动添加事务
        log.info("JBM切面事务配置完成：只对写操作添加事务，查询操作不添加事务");
        return new TransactionInterceptor(transactionManager, source);
    }

    @Bean
    public Advisor txAdviceAdvisor() {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(targetPackages);
        return new DefaultPointcutAdvisor(pointcut, txAdvice());
    }


    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                annotationMetadata.getAnnotationAttributes(EnableTransactionAdviceManagement.class.getName()));
        this.targetPackages = attributes.getString("targetPackages");
    }
}
