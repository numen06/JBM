package com.jbm.cluster.auth.service;

import com.google.common.collect.Lists;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.service.ILoginAuthenticate;
import com.jbm.framework.metadata.bean.ResultBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Created wesley.zhang
 * @Date 2022/5/27 0:59
 * @Description TODO
 */
@Slf4j
@Component
public class LoginAuthenticateFallback implements FallbackFactory<ILoginAuthenticate> {

    @Override
    public ILoginAuthenticate create(Throwable throwable) {
        log.error("认证服务调用失败:{}", throwable.getMessage());
        return new ILoginAuthenticate() {
            @Override
            public ResultBody<JbmLoginUser> login(String username, String password, String loginType) {
                return ResultBody.error("获取用户失败:" + throwable.getMessage());
            }

            @Override
            public List<LoginType> getLoginType() {
                return Lists.newArrayList(LoginType.PASSWORD);
            }
        };
    }
}
