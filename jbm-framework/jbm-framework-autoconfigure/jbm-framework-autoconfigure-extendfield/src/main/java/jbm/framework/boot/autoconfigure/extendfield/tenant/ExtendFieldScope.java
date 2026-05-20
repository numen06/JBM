package jbm.framework.boot.autoconfigure.extendfield.tenant;

import jbm.framework.boot.autoconfigure.extendfield.ExtendFieldProperties;
import org.springframework.util.StringUtils;

/**
 * 多租户下 Redis / 本地配置的 form 作用域键：{@code tenantId:formCode}；
 * 无请求租户且开启默认模块时使用 {@code defaultTenantId:formCode}。
 */
public final class ExtendFieldScope {

    private ExtendFieldScope() {
    }

    public static String scopedFormCode(ExtendFieldProperties properties, String tenantId, String formCode) {
        if (formCode == null || formCode.isEmpty()) {
            return formCode;
        }
        if (properties == null || properties.getTenant() == null || !properties.getTenant().isEnabled()) {
            return formCode;
        }
        String effectiveTenant = StringUtils.hasText(tenantId) ? tenantId.trim() : null;
        if (!StringUtils.hasText(effectiveTenant) && properties.getTenant().isUseDefaultWhenMissing()) {
            effectiveTenant = ExtendFieldTenantResolver.resolveTenantId(properties);
        }
        if (!StringUtils.hasText(effectiveTenant)) {
            return formCode;
        }
        return effectiveTenant + ":" + formCode;
    }

    public static String scopedFormCode(ExtendFieldProperties properties, String formCode) {
        return scopedFormCode(properties, ExtendFieldTenantResolver.resolveTenantId(properties), formCode);
    }
}
