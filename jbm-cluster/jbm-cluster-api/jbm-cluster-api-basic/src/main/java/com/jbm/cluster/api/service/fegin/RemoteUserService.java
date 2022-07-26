package com.jbm.cluster.api.service.fegin;

import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.factory.RemoteUserFallbackFactory;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 用户服务
 *
 * @author wesley.zhang
 */
@FeignClient(contextId = "remoteUserService", value = JbmClusterConstants.BASE_SERVER,
        fallbackFactory = RemoteUserFallbackFactory.class)
public interface RemoteUserService {
    /**
     * 通过用户名查询用户信息
     *
     * @param username 用户名
     * @param source   请求来源
     * @return 结果
     */
    @GetMapping("/user/info/{username}")
    public ResultBody<JbmLoginUser> getUserInfo(@PathVariable("username") String username, @RequestHeader(JbmSecurityConstants.FROM_SOURCE) String source);

    /**
     * 注册用户信息
     *
     * @param baseUser 用户信息
     * @param source   请求来源
     * @return 结果
     */
    @PostMapping("/user/register")
    public ResultBody<Boolean> registerUserInfo(@RequestBody BaseUser baseUser, @RequestHeader(JbmSecurityConstants.FROM_SOURCE) String source);
}
