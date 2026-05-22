package com.jbm.cluster.api.factory;

import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.service.feign.RemoteUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteUserFallbackFactory implements FallbackFactory<RemoteUserService> {
    private static final Logger log = LoggerFactory.getLogger(RemoteUserFallbackFactory.class);

    @Override
    public RemoteUserService create(Throwable throwable) {
        log.error("用户服务调用失败:{}", throwable.getMessage());
        return new RemoteUserService() {
            @Override
            public JbmLoginUser getUserInfo(String username, String source) {
                throw new RuntimeException("获取用户失败:" + throwable.getMessage(), throwable);
            }

            @Override
            public Boolean registerUserInfo(BaseUser baseUser, String source) {
                throw new RuntimeException("注册用户失败:" + throwable.getMessage(), throwable);
            }
        };
    }
}