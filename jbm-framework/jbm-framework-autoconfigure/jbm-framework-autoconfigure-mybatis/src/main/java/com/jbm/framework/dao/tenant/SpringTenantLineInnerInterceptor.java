package com.jbm.framework.dao.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.springframework.context.ApplicationContext;

/**
 * 租户拦截器
 * @author wesley
 */
public class SpringTenantLineInnerInterceptor extends TenantLineInnerInterceptor {

    private final TenantProperties tenantProperties;

    private final ApplicationContext applicationContext;

    public SpringTenantLineInnerInterceptor(TenantProperties tenantProperties, ApplicationContext applicationContext) {
        this.tenantProperties = tenantProperties;
        this.applicationContext = applicationContext;
        this.setTenantLineHandler(applicationContext.getBean(TenantLineHandler.class));
    }

}
