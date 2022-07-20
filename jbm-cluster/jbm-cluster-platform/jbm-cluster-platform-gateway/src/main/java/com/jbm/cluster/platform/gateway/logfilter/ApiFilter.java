package com.jbm.cluster.platform.gateway.logfilter;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.jbm.cluster.api.entitys.basic.BaseApi;
import com.jbm.cluster.api.model.gateway.GatewayLogInfo;
import com.jbm.cluster.api.service.fegin.client.BaseApiServiceClient;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 过滤请求中的服务
 */
@Component
public class ApiFilter implements AccessLogFilter {
    @Autowired
    private BaseApiServiceClient baseApiServiceClient;


    @Override
    public void filter(GatewayLogInfo gatewayLogInfo, Map<String, String> headers) {
        //不存在服务ID直接退出
        if (!ObjectUtil.isAllNotEmpty(gatewayLogInfo.getServiceId(), baseApiServiceClient)) {
            return;
        }
        String realPath = gatewayLogInfo.getPath();
        realPath = StrUtil.removePrefix(realPath, "/");
        realPath = "/" + StrUtil.subAfter(realPath, "/", false);
        try {
            BaseApi baseApi = appLoadingCache.get(gatewayLogInfo.getServiceId() + realPath);
            if (ObjectUtil.isEmpty(baseApi)) {
                return;
            }
            gatewayLogInfo.setOperationType(baseApi.getBusinessScope());
            gatewayLogInfo.setApiName(baseApi.getApiName());
        } catch (Exception e) {

        }
    }

    LoadingCache<String, BaseApi> appLoadingCache = Caffeine.newBuilder()
            //一小时没有读取释放
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, BaseApi>() {
                @Override
                public @Nullable BaseApi load(@NonNull String path) throws Exception {
                    String serviceId = StrUtil.subBefore(path, "/", false);
                    String realPath = StrUtil.removePrefix(path, serviceId);
                    return baseApiServiceClient.findApiByPath(serviceId, realPath).getResult();
                }
            });
}
