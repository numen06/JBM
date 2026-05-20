package jbm.framework.boot.autoconfigure.extendfield.tenant;

import jbm.framework.boot.autoconfigure.extendfield.ExtendFieldProperties;
import org.springframework.util.StringUtils;

/**
 * 解析当前生效的扩展字段租户：请求上下文优先，无租户时可回落默认模块。
 */
public final class ExtendFieldTenantResolver {

    /** 未传租户时常用的默认模块 ID（与库表 tenant_id、Redis 作用域一致）。 */
    public static final String DEFAULT_MODULE_TENANT_ID = "0";

    private ExtendFieldTenantResolver() {
    }

    /**
     * 未启用多租户时返回 {@code null}；启用时返回上下文租户或配置的默认租户。
     */
    public static String resolveTenantId(ExtendFieldProperties properties) {
        if (properties == null || properties.getTenant() == null || !properties.getTenant().isEnabled()) {
            return null;
        }
        String fromContext = ExtendFieldTenantContext.getTenantId();
        if (StringUtils.hasText(fromContext)) {
            return fromContext.trim();
        }
        if (properties.getTenant().isUseDefaultWhenMissing()) {
            String fallback = properties.getTenant().getDefaultTenantId();
            return StringUtils.hasText(fallback) ? fallback.trim() : DEFAULT_MODULE_TENANT_ID;
        }
        return null;
    }

    public static Long resolveTenantIdAsLong(ExtendFieldProperties properties) {
        String tenantId = resolveTenantId(properties);
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        try {
            return Long.parseLong(tenantId.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("非法扩展字段租户 ID: " + tenantId);
        }
    }
}
