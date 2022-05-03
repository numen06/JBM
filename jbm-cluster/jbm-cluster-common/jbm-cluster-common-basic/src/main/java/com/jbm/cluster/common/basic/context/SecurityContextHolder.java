package com.jbm.cluster.common.basic.context;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.jbm.cluster.core.constant.JbmSecurityConstants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 获取当前线程变量中的 用户id、用户名称、Token等信息
 * 注意： 必须在网关通过请求头的方法传入，同时在HeaderInterceptor拦截器设置值。 否则这里无法获取
 *
 * @author wesley.zhang
 */
public class SecurityContextHolder {
    private static final TransmittableThreadLocal<Map<String, Object>> THREAD_LOCAL = new TransmittableThreadLocal<>();

    public static void set(String key, Object value) {
        Map<String, Object> map = getLocalMap();
        map.put(key, value == null ? StrUtil.EMPTY : value);
    }

    public static String get(String key) {
        Map<String, Object> map = getLocalMap();
        return StrUtil.toStringOrNull(map.getOrDefault(key, StrUtil.EMPTY));
    }

    public static <T> T get(String key, Class<T> clazz) {
        Map<String, Object> map = getLocalMap();
        return cast(map.getOrDefault(key, null));
    }

    public static <T> T cast(Object obj) {
        return (T) obj;
    }

    public static Map<String, Object> getLocalMap() {
        Map<String, Object> map = THREAD_LOCAL.get();
        if (map == null) {
            map = new ConcurrentHashMap<String, Object>();
            THREAD_LOCAL.set(map);
        }
        return map;
    }

    public static void setLocalMap(Map<String, Object> threadLocalMap) {
        THREAD_LOCAL.set(threadLocalMap);
    }

    public static Long getUserId() {

        return Convert.toLong(get(JbmSecurityConstants.DETAILS_USER_ID), 0L);
    }

    public static void setUserId(String account) {
        set(JbmSecurityConstants.DETAILS_USER_ID, account);
    }

    public static String getUserName() {
        return get(JbmSecurityConstants.DETAILS_USERNAME);
    }

    public static void setUserName(String username) {
        set(JbmSecurityConstants.DETAILS_USERNAME, username);
    }

    public static String getUserKey() {
        return get(JbmSecurityConstants.USER_KEY);
    }

    public static void setUserKey(String userKey) {
        set(JbmSecurityConstants.USER_KEY, userKey);
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }
}
