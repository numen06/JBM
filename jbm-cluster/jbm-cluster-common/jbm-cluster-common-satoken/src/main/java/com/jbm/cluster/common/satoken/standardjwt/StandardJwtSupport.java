package com.jbm.cluster.common.satoken.standardjwt;

import cn.hutool.extra.spring.SpringUtil;

public final class StandardJwtSupport {

    private StandardJwtSupport() {
    }

    public static StandardJwtPrincipal verify(String token) {
        try {
            StandardJwtVerifier verifier = SpringUtil.getBean(StandardJwtVerifier.class);
            if (verifier == null || !verifier.isEnabled()) {
                return null;
            }
            return verifier.verify(token);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static StandardJwtPrincipal bind(String token) {
        StandardJwtPrincipal current = StandardJwtContext.get();
        if (current != null && token != null && token.equals(current.getToken())) {
            return current;
        }
        StandardJwtPrincipal principal = verify(token);
        if (principal != null) {
            StandardJwtContext.set(principal);
        }
        return principal;
    }

    public static String resolveLoginId(String token) {
        StandardJwtPrincipal principal = bind(token);
        if (principal == null) {
            return null;
        }
        return principal.getLoginId();
    }
}
