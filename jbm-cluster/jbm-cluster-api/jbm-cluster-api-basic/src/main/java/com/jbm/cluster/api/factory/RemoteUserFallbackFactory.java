package com.jbm.cluster.api.factory;

import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.service.feign.RemoteUserService;
import com.jbm.framework.metadata.bean.ResultBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 用户服务降级处理
 *
 * @author wesley.zhang
 */
@Component
public class RemoteUserFallbackFactory implements FallbackFactory<RemoteUserService> {
    private static final Logger log = LoggerFactory.getLogger(RemoteUserFallbackFactory.class);

    @Override
    public RemoteUserService create(Throwable throwable) {
        log.error("用户服务调用失败:{}", throwable.getMessage());
        return new RemoteUserService() {
            @Override
            public ResultBody<JbmLoginUser> getUserInfo(String username, String source) {
                return ResultBody.error("获取用户失败:" + throwable.getMessage());
            }

            @Override
            public ResultBody<Boolean> registerUserInfo(BaseUser baseUser, String source) {
                return ResultBody.error("注册用户失败:" + throwable.getMessage());
            }
        };
    }
}
