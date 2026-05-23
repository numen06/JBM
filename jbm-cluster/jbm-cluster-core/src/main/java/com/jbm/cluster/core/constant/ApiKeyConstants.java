package com.jbm.cluster.core.constant;

/**
 * API Key 与开发者申请状态常量
 */
public final class ApiKeyConstants {

    private ApiKeyConstants() {
    }

    /** API Key 禁用 */
    public static final int API_KEY_STATUS_DISABLED = 0;
    /** API Key 启用 */
    public static final int API_KEY_STATUS_ENABLED = 1;

    /** 开发者待审批 */
    public static final int DEVELOPER_STATUS_PENDING = 0;
    /** 开发者正常 */
    public static final int DEVELOPER_STATUS_ACTIVE = 1;
    /** 开发者锁定/拒绝 */
    public static final int DEVELOPER_STATUS_LOCKED = 2;

    /** 授权启用 */
    public static final int AUTH_STATUS_ENABLED = 1;
    /** 授权禁用 */
    public static final int AUTH_STATUS_DISABLED = 2;
}
