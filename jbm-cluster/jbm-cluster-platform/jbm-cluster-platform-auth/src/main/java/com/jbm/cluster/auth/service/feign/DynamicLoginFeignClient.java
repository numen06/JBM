package com.jbm.cluster.auth.service.feign;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.auth.service.LoginAuthenticate;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import org.apache.ibatis.jdbc.Null;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * @Created wesley.zhang
 * @Date 2022/5/19 16:00
 * @Description TODO
 */
@Service
public class DynamicLoginFeignClient {

    private FeignClientBuilder feignClientBuilder;

    private RedisService redisService;

    private final static String LOGIN_AUTHENTICATE = "LoginAuthenticate:";


    public DynamicLoginFeignClient(@Autowired ApplicationContext appContext) {
        this.feignClientBuilder = new FeignClientBuilder(appContext);
    }

    private String findLoginServiceName(LoginType loginType) {
        try {
            String seviceName = redisService.getCacheObject(LOGIN_AUTHENTICATE + loginType.toString());
            if (StrUtil.isEmpty(seviceName)) {
                return JbmClusterConstants.AUTH_SERVER;
            }
            return seviceName;
        } catch (NullPointerException e) {
            return JbmClusterConstants.AUTH_SERVER;
        }

    }

    public LoginAuthenticate getFeginLoginAuthenticate(LoginType loginType) {
        return this.getFeginLoginAuthenticate(this.findLoginServiceName(loginType));
    }

    public LoginAuthenticate getFeginLoginAuthenticate(String seviceName) {
        return this.feignClientBuilder.forType(LoginAuthenticate.class, seviceName).build();
    }
}
