package com.jbm.cluster.common.satoken.standardjwt;

import com.jbm.cluster.api.model.auth.JbmLoginUser;

import java.util.Map;

public final class StandardJwtContext {

    private static final ThreadLocal<StandardJwtPrincipal> PRINCIPAL = new ThreadLocal<StandardJwtPrincipal>();

    private StandardJwtContext() {
    }

    public static void set(StandardJwtPrincipal principal) {
        PRINCIPAL.set(principal);
    }

    public static StandardJwtPrincipal get() {
        return PRINCIPAL.get();
    }

    public static void clear() {
        PRINCIPAL.remove();
    }

    public static Map<String, Object> getClaims() {
        StandardJwtPrincipal principal = get();
        return principal == null ? null : principal.getClaims();
    }

    public static JbmLoginUser getLoginUser() {
        StandardJwtPrincipal principal = get();
        return principal == null ? null : principal.getLoginUser();
    }
}
