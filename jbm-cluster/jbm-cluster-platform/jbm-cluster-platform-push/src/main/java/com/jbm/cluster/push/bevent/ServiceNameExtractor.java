package com.jbm.cluster.push.bevent;

/**
 * @author wesley
 */
public class ServiceNameExtractor {

    public static String extractServiceName(String url) {
        if (url == null) {
            return null;
        }
        if (!url.startsWith("feign://")) {
            // 非 feign 协议，可能是 HTTP，不走服务发现重试逻辑
            return null;
        }
        // remove "feign://"
        url = url.substring(8);
        int slashIndex = url.indexOf('/');
        return slashIndex == -1 ? url : url.substring(0, slashIndex);
    }

    public static String getEnqueueName(String url) {
        String serviceName = extractServiceName(url);
        // 🎯 关键：确定“队列分组键”
        // - feign://user-service → 用 "user-service"
        // - http://xxx → 用完整 URL（或 host+path，根据你的排序粒度）
        return serviceName != null ? serviceName : url;
    }
}