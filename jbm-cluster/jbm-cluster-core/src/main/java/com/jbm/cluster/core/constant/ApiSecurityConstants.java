package com.jbm.cluster.core.constant;

/**
 * Open API / login RSA security headers and constants.
 */
public final class ApiSecurityConstants {

    private ApiSecurityConstants() {
    }

    public static final String APP_ID = "X-App-Id";

    public static final String TIMESTAMP = "X-Timestamp";

    public static final String SIGNATURE = "X-Signature";

    public static final String PASSWORD_ENCRYPTED = "X-Password-Encrypted";

    public static final String PASSWORD_ENCRYPTED_VALUE = "true";

    public static final boolean LOGIN_PASSWORD_ENCRYPT_REQUIRED = true;
}