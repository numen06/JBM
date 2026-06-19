package com.jbm.cluster.common.satoken.standardjwt;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.model.auth.JbmLoginUser;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StandardJwtLoginConverter {

    private final StandardJwtProperties properties;

    public StandardJwtLoginConverter(StandardJwtProperties properties) {
        this.properties = properties;
    }

    public String resolveLoginId(Map<String, Object> claims) {
        String loginId = stringClaim(claims, properties.getLoginIdClaim());
        if (StrUtil.isNotBlank(loginId)) {
            return loginId;
        }
        String userType = StrUtil.blankToDefault(stringClaim(claims, properties.getUserTypeClaim()), "user");
        Long appId = longClaim(claims, properties.getAppIdClaim());
        Long userId = longClaim(claims, properties.getUserIdClaim());
        if (userId == null) {
            return null;
        }
        return userType + ":" + (appId == null ? 0L : appId) + ":" + userId;
    }

    public JbmLoginUser toLoginUser(String token, Map<String, Object> claims) {
        JbmLoginUser loginUser = new JbmLoginUser();
        loginUser.setToken(token);
        loginUser.setUserId(longClaim(claims, properties.getUserIdClaim()));
        loginUser.setUsername(stringClaim(claims, properties.getUsernameClaim()));
        loginUser.setAccount(loginUser.getUsername());
        loginUser.setClientId(stringClaim(claims, properties.getClientIdClaim()));
        loginUser.setAppId(longClaim(claims, properties.getAppIdClaim()));
        loginUser.setUserType(StrUtil.blankToDefault(stringClaim(claims, properties.getUserTypeClaim()), "user"));
        loginUser.setCompanyId(longClaim(claims, properties.getTenantIdClaim()));
        loginUser.setMenuPermission(stringSetClaim(claims, properties.getPermissionsClaim()));
        loginUser.setRoles(stringSetClaim(claims, properties.getRolesClaim()));
        return loginUser;
    }

    private static String stringClaim(Map<String, Object> claims, String name) {
        if (claims == null || StrUtil.isBlank(name)) {
            return null;
        }
        return Convert.toStr(claims.get(name), null);
    }

    private static Long longClaim(Map<String, Object> claims, String name) {
        if (claims == null || StrUtil.isBlank(name)) {
            return null;
        }
        Object value = claims.get(name);
        if (value == null) {
            return null;
        }
        return Convert.toLong(value, null);
    }

    private static Set<String> stringSetClaim(Map<String, Object> claims, String name) {
        Set<String> values = new HashSet<String>();
        if (claims == null || StrUtil.isBlank(name)) {
            return values;
        }
        Object raw = claims.get(name);
        if (raw instanceof Collection) {
            for (Object item : (Collection<?>) raw) {
                String value = Convert.toStr(item, null);
                if (StrUtil.isNotBlank(value)) {
                    values.add(value);
                }
            }
            return values;
        }
        String text = Convert.toStr(raw, null);
        if (StrUtil.isBlank(text)) {
            return values;
        }
        for (String item : StrUtil.split(text, ',')) {
            if (StrUtil.isNotBlank(item)) {
                values.add(item.trim());
            }
        }
        return values;
    }
}
