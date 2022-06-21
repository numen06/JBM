package com.jbm.cluster.auth.service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.service.ILoginAuthenticate;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Map;


/**
 * @Created wesley.zhang
 * @Date 2022/5/19 16:00
 * @Description TODO
 */
@Service
public class DynamicLoginFeignClient {

    private FeignClientBuilder feignClientBuilder;

    @Autowired
    private RedisService redisService;
    @Autowired
    private LoginAuthenticateFallback loginAuthenticateFallback;
    @Autowired
    private ApplicationContext applicationContext;


    public DynamicLoginFeignClient(@Autowired ApplicationContext appContext) {
        this.feignClientBuilder = new FeignClientBuilder(appContext);
    }

    private String findLoginServiceName(LoginType loginType) {
        try {
            String seviceName = redisService.getCacheObject(JbmSecurityConstants.LOGIN_AUTHENTICATE_KEY + loginType.toString());
            if (StrUtil.isEmpty(seviceName)) {
                return JbmClusterConstants.AUTH_SERVER;
            }
            return seviceName;
        } catch (NullPointerException e) {
            return JbmClusterConstants.AUTH_SERVER;
        }

    }

    public ILoginAuthenticate findLoalLoginAuthenticate(LoginType loginType) {
        Map<String, ILoginAuthenticate> loginBeans = applicationContext.getBeansOfType(ILoginAuthenticate.class);
        if (MapUtil.isEmpty(loginBeans)) {
            return null;
        }
        for (ILoginAuthenticate authenticate : loginBeans.values()) {
            if (authenticate.getLoginType().equals(loginType)) {
                return authenticate;
            }
        }
        return null;
    }


    public ILoginAuthenticate getFeginLoginAuthenticate(LoginType loginType) {
        ILoginAuthenticate authenticate = this.findLoalLoginAuthenticate(loginType);
        if (ObjectUtil.isNotEmpty(authenticate)) {
            return authenticate;
        }
        return this.getFeginLoginAuthenticate(this.findLoginServiceName(loginType));
    }

    public ILoginAuthenticate getFeginLoginAuthenticate(String seviceName) {
        return this.feignClientBuilder.forType(ILoginAuthenticate.class, seviceName).build();
    }
}
