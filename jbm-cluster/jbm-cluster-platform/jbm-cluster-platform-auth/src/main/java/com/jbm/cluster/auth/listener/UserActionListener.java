package com.jbm.cluster.auth.listener;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.listener.SaTokenListener;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.auth.SysUserOnline;
import com.jbm.cluster.common.basic.utils.IpUtils;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.core.constant.JbmCacheConstants;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import jbm.framework.web.ServletUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 用户行为 侦听器的实现
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class UserActionListener implements SaTokenListener {

    private final SaTokenConfig tokenConfig;

    @Autowired
    private RedisService redisService;

    /**
     * 每次登录时触发
     */
    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginModel loginModel) {
        UserAgent userAgent = UserAgentUtil.parse(ServletUtils.getRequest().getHeader("User-Agent"));
        String ip = ServletUtils.getClientIP();
        JbmLoginUser user = LoginHelper.getLoginUser();
        //新版本直接传入
        // String tokenValue = StpUtil.getTokenValueByLoginId(loginId);
        SysUserOnline userOnline = new SysUserOnline();
        userOnline.setIpaddr(ip);
        userOnline.setLoginLocation(IpUtils.getAddressByIP(ip));
        userOnline.setBrowser(userAgent.getBrowser().getName());
        userOnline.setOs(userAgent.getOs().getName());
        userOnline.setLoginTime(DateTime.now());
        userOnline.setTokenId(tokenValue);
        userOnline.setExpiredTime(DateUtil.offsetSecond(userOnline.getLoginTime(), loginModel.getTimeout().intValue()));
        userOnline.setUserName(user.getUsername());
        if (ObjectUtil.isNotNull(user.getDeptName())) {
            userOnline.setDeptName(user.getDeptName());
        }
        redisService.setCacheObject(JbmCacheConstants.ONLINE_TOKEN_KEY + tokenValue, userOnline, tokenConfig.getTimeout(), TimeUnit.SECONDS);
        log.info("user doLogin, useId:{}, token:{}", loginId, tokenValue);
    }

    /**
     * 每次注销时触发
     */
    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        redisService.deleteObject(JbmCacheConstants.ONLINE_TOKEN_KEY + tokenValue);
        log.info("user doLogout, useId:{}, token:{}", loginId, tokenValue);
    }

    /**
     * 每次被踢下线时触发
     */
    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        redisService.deleteObject(JbmCacheConstants.ONLINE_TOKEN_KEY + tokenValue);
        log.info("user doLogoutByLoginId, useId:{}, token:{}", loginId, tokenValue);
    }

    /**
     * 每次被顶下线时触发
     */
    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        redisService.deleteObject(JbmCacheConstants.ONLINE_TOKEN_KEY + tokenValue);
        log.info("user doReplaced, useId:{}, token:{}", loginId, tokenValue);
    }

    /**
     * 每次被封禁时触发
     */
    @Override
    public void doDisable(String loginType, Object loginId, long disableTime) {
    }

    /**
     * 每次被解封时触发
     */
    @Override
    public void doUntieDisable(String loginType, Object loginId) {
    }

    /**
     * 每次创建Session时触发
     */
    @Override
    public void doCreateSession(String id) {
    }

    /**
     * 每次注销Session时触发
     */
    @Override
    public void doLogoutSession(String id) {
    }


}
