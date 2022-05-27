package com.jbm.cluster.common.basic.configuration;

import cn.hutool.extra.spring.SpringUtil;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.service.ILoginAuthenticate;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @Created wesley.zhang
 * @Date 2022/5/26 13:46
 * @Description TODO
 */
@Slf4j
public class LoginAssemblyConfiguration {

    @Autowired
    private RedisService redisService;
    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void assembly() {
        Map<String, ILoginAuthenticate> loginAuthenticateMap = applicationContext.getBeansOfType(ILoginAuthenticate.class);
        loginAuthenticateMap.forEach(new BiConsumer<String, ILoginAuthenticate>() {
            @Override
            public void accept(String beanName, ILoginAuthenticate iLoginAuthenticate) {
                LoginType loginType = iLoginAuthenticate.getLoginType();
                if (loginType == null)
                    return;
                String key = JbmSecurityConstants.LOGIN_AUTHENTICATE_KEY + loginType.toString();
                log.info("装配登录注册器[{}],服务名[{}]", loginType, SpringUtil.getApplicationName());
                redisService.setCacheObject(key, SpringUtil.getApplicationName());
            }
        });
    }

}
