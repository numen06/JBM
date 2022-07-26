package com.jbm.framework.masterdata.code.transaction;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurationSelector;

import java.lang.annotation.*;

/**
 * @author: create by wesley
 * @date:2019/4/28
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({TransactionManagementConfigurationSelector.class, TransactionAdviceConfig.class})
@EnableTransactionManagement
public @interface EnableTransactionAdviceManagement {
    String targetPackages() default "execution (* com.***.service.*.*(..))";

    boolean proxyTargetClass() default false;

    AdviceMode mode() default AdviceMode.PROXY;

    int order() default 2147483647;
}
