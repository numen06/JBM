package jbm.framework.boot.autoconfigure.extendfield.tenant;

/**
 * 当前请求的扩展字段租户（由 {@link ExtendFieldTenantFilter} 或业务代码写入）。
 */
public final class ExtendFieldTenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private ExtendFieldTenantContext() {
    }

    public static void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            CURRENT.remove();
        } else {
            CURRENT.set(tenantId.trim());
        }
    }

    public static String getTenantId() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
