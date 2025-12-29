package com.jbm.framework.dao.mybatis.sqlInjector;

/**
 * 安全持有当前线程正在执行的 MappedStatement ID（如 "com.xxx.mapper.UserMapper.selectById"）
 * 由 SqlSessionInterceptor 设置，由 WhitelistLogImpl 读取。
 * @author wesley
 */
public class MappedStatementHolder {

    private static final ThreadLocal<String> MS_ID_HOLDER = ThreadLocal.withInitial(() -> null);

    public static void set(String msId) {
        MS_ID_HOLDER.set(msId);
    }

    public static String get() {
        return MS_ID_HOLDER.get();
    }

    public static void clear() {
        MS_ID_HOLDER.remove();
    }
}