package com.jbm.cluster.job.util;

import cn.hutool.core.util.StrUtil;

/**
 * 定时任务工具（保留白名单等无 Quartz 依赖的工具方法）。
 */
public final class ScheduleUtils {

    private ScheduleUtils() {
    }

    /**
     * 检查包名是否为白名单配置
     *
     * @param invokeTarget 目标字符串
     * @return 结果
     */
    public static boolean whiteList(String invokeTarget) {
        String packageName = StrUtil.subBefore(invokeTarget, "(", false);
        int count = StrUtil.count(packageName, ".");
        if (count > 1) {
            return StrUtil.containsAnyIgnoreCase(invokeTarget, "com.jbm");
        }
        return true;
    }
}
