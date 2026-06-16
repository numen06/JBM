package com.jbm.cluster.common.mysql.event;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.jbm.cluster.api.entitys.basic.BaseAccountLogs;
import com.jbm.cluster.api.event.auth.LoginFailureEvent;
import com.jbm.cluster.api.event.auth.LoginSuccessEvent;
import com.jbm.cluster.api.event.auth.LogoutEvent;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.common.mysql.service.BaseAccountService;
import com.jbm.cluster.core.constant.JbmCacheConstants;
import com.jbm.framework.exceptions.user.UserException;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class LoginDatabaseHook {

    @Autowired
    private BaseAccountService baseAccountService;
    @Autowired
    private RedisService redisService;

    @EventListener
    public void onLoginSuccess(LoginSuccessEvent event) {
        JbmLoginUser user = event.getLoginUser();
        if (user == null) {
            return;
        }
        try {
            BaseAccountLogs accountLog = new BaseAccountLogs();
            accountLog.setUserId(user.getUserId());
            accountLog.setAccount(user.getAccount());
            accountLog.setAccountType(user.getAccountType());
            accountLog.setDomain(user.getUserType());
            accountLog.setLoginIp(event.getIp());
            accountLog.setLoginAgent(event.getUserAgent());
            if (StrUtil.isNotBlank(event.getUserAgent())) {
                UserAgent ua = UserAgentUtil.parse(event.getUserAgent());
                accountLog.setBrowser(ua.getBrowser().getName() + " " + ua.getVersion());
                accountLog.setOs(ua.getOs().getName());
            }
            accountLog.setLoginStatus(true);
            accountLog.setLoginTime(DateTime.now());
            baseAccountService.addLoginLog(accountLog);
        } catch (Exception e) {
            log.error("写入登录成功日志失败 userId={}", user.getUserId(), e);
        }
        if (StrUtil.isNotBlank(user.getUsername())) {
            redisService.deleteObject(JbmCacheConstants.LOGIN_ERROR + user.getUsername());
        }
    }

    @EventListener
    public void onLoginFailure(LoginFailureEvent event) {
        if (StrUtil.isBlank(event.getUsername())) {
            return;
        }
        String errorKey = JbmCacheConstants.LOGIN_ERROR + event.getUsername();
        Integer errorNumber = redisService.getCacheObject(errorKey);
        errorNumber = (errorNumber == null) ? 1 : errorNumber + 1;
        if (errorNumber >= JbmCacheConstants.LOGIN_ERROR_NUMBER) {
            redisService.setCacheObject(errorKey, errorNumber,
                    Long.valueOf(JbmCacheConstants.LOGIN_ERROR_LIMIT_TIME), TimeUnit.MINUTES);
            log.warn("用户 {} 登录失败次数达上限 {}", event.getUsername(), errorNumber);
        } else {
            redisService.setCacheObject(errorKey, errorNumber);
        }
    }

    public void assertNotLocked(String username, String loginTypeName) {
        if (StrUtil.isBlank(username)) {
            return;
        }
        String errorKey = JbmCacheConstants.LOGIN_ERROR + username;
        Integer errorNumber = redisService.getCacheObject(errorKey);
        if (errorNumber != null && errorNumber >= JbmCacheConstants.LOGIN_ERROR_NUMBER) {
            throw new UserException("user.password.retry.limit.exceed",
                    new Object[]{JbmCacheConstants.LOGIN_ERROR_LIMIT_TIME},
                    StrUtil.format("密码错误次数过多，账户锁定{}分钟", JbmCacheConstants.LOGIN_ERROR_LIMIT_TIME));
        }
    }

    @EventListener
    public void onLogout(LogoutEvent event) {
        log.info("用户登出 loginId={}, token={}", event.getLoginId(), event.getTokenValue());
    }
}
