package com.jbm.framework.dao.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Table;
import org.springframework.context.ApplicationContext;

/**
 * 租户拦截器
 *
 * @author wesley
 */
public class SpringTenantLineInnerInterceptor extends TenantLineInnerInterceptor {

    private TenantProperties tenantProperties;

    private ApplicationContext applicationContext;

    public SpringTenantLineInnerInterceptor(TenantProperties tenantProperties, ApplicationContext applicationContext) {
        this.tenantProperties = tenantProperties;
        this.applicationContext = applicationContext;
        this.setTenantLineHandler(applicationContext.getBean(TenantLineHandler.class));
    }

    public SpringTenantLineInnerInterceptor(TenantLineHandler tenantLineHandler) {
        super(tenantLineHandler);
    }

    @Override
    public Expression buildTableExpression(final Table table, final Expression where, final String whereSegment) {
        if (this.getTenantLineHandler().ignoreTable(table.getName())) {
            return null;
        }
        if (this.getTenantLineHandler().getTenantId() == null) {
            return null;
        }
        return new EqualsTo(getAliasColumn(table), this.getTenantLineHandler().getTenantId());
    }


}
