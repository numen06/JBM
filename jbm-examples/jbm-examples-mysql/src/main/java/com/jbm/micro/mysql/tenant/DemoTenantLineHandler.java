package com.jbm.micro.mysql.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 演示用多租户：通过 {@link #setTenantId(Long)} / {@link #clear()} 在测试中模拟当前租户。
 * 无租户上下文时 {@link #getTenantId()} 为 {@code null}，与框架 {@link com.jbm.framework.dao.tenant.SpringTenantLineInnerInterceptor} 行为一致（不追加条件）。
 */
@Component
public class DemoTenantLineHandler implements TenantLineHandler {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        CURRENT.set(tenantId);
    }

    public static void clear() {
        CURRENT.remove();
    }

    @Override
    public Expression getTenantId() {
        Long id = CURRENT.get();
        return id == null ? null : new LongValue(id);
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (tableName == null) {
            return true;
        }
        String t = tableName.toLowerCase(Locale.ROOT);
        return "md_sample".equals(t)
                || "md_form_row".equals(t)
                || "databasechangelog".equals(t)
                || "databasechangeloglock".equals(t);
    }
}
