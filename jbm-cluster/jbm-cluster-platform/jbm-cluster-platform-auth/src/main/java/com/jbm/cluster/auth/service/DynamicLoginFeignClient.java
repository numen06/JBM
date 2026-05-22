package com.jbm.cluster.auth.service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.service.ILoginAuthenticate;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.framework.exceptions.ServiceException;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 登录认证路由：优先本地 ILoginAuthenticate，可选 Redis 指定远程服务。
 */
@Service
public class DynamicLoginFeignClient {

    private final FeignClientBuilder feignClientBuilder;

    @Autowired
    private RedisService redisService;
    @Autowired
    private ApplicationContext applicationContext;

    public DynamicLoginFeignClient(@Autowired ApplicationContext appContext) {
        this.feignClientBuilder = new FeignClientBuilder(appContext);
    }

    private String findLoginServiceName(LoginType loginType) {
        try {
            String serviceName = redisService.getCacheObject(JbmSecurityConstants.LOGIN_AUTHENTICATE_KEY + loginType.toString());
            if (StrUtil.isEmpty(serviceName)) {
                return null;
            }
            return serviceName;
        } catch (NullPointerException e) {
            return null;
        }
    }

    public ILoginAuthenticate findLoalLoginAuthenticate(LoginType loginType) {
        Map<String, ILoginAuthenticate> loginBeans = applicationContext.getBeansOfType(ILoginAuthenticate.class);
        if (MapUtil.isEmpty(loginBeans)) {
            return null;
        }
        for (ILoginAuthenticate authenticate : loginBeans.values()) {
            if (authenticate.getLoginType().contains(loginType)) {
                return authenticate;
            }
        }
        return null;
    }

    public ILoginAuthenticate getFeginLoginAuthenticate(LoginType loginType) {
        ILoginAuthenticate authenticate = findLoalLoginAuthenticate(loginType);
        if (ObjectUtil.isNotEmpty(authenticate)) {
            return authenticate;
        }
        String serviceName = findLoginServiceName(loginType);
        if (StrUtil.isNotBlank(serviceName) && !JbmClusterConstants.AUTH_SERVER.equals(serviceName)) {
            return feignClientBuilder.forType(ILoginAuthenticate.class, serviceName).build();
        }
        throw new ServiceException("不支持的登录类型: " + loginType);
    }
}
