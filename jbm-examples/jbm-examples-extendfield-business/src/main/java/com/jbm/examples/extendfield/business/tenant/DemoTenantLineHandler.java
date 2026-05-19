package com.jbm.examples.extendfield.business.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DemoTenantLineHandler implements TenantLineHandler {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

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
        return "md_extend_order".equals(t)
                || "databasechangelog".equals(t)
                || "databasechangeloglock".equals(t);
    }
}
