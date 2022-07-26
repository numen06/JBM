package com.jbm.framework.exceptions.auth;

import org.apache.commons.lang3.StringUtils;

/**
 * 未能通过的权限认证异常
 *
 * @author wesley.zhang
 */
public class NotPermissionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NotPermissionException(String permission) {
        super(permission);
    }

    public NotPermissionException(String[] permissions) {
        super(StringUtils.join(permissions, ","));
    }
}
