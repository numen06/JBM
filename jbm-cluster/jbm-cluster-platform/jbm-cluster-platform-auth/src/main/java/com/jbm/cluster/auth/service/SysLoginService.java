package com.jbm.cluster.auth.service;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.constants.RequestDeviceType;
import com.jbm.cluster.api.constants.UserType;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.model.auth.AccessTokenResult;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.auth.form.RegisterBody;
import com.jbm.cluster.auth.service.feign.BaseUserServiceClient;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.common.satoken.utils.SecurityUtils;
import com.jbm.cluster.core.constant.JbmCacheConstants;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.exceptions.user.UserException;
import com.jbm.framework.metadata.bean.ResultBody;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 登录校验方法
 *
 * @author wesley.zhang
 */
@Service
public class SysLoginService {
    @Autowired(required = false)
    private BaseUserServiceClient baseUserServiceClient;
    //    @DubboReference
//    private RemoteLogService remoteLogService;
    //    @DubboReference
//    private RemoteUserService remoteUserService;

    @Autowired
    private RedisService redisService;

    public JbmLoginUser conventJbmLoginUser(BaseUser baseUser) {
        JbmLoginUser jbmLoginUser = new JbmLoginUser();
        BeanUtil.copyProperties(baseUser, jbmLoginUser);
        return jbmLoginUser;
    }

    /**
     * 登录
     */
    public AccessTokenResult login(String username, String password) {
        ResultBody<UserAccount> userAccountResultBody = baseUserServiceClient.userLogin(username);
        ResultBody<BaseUser> baseUserResultBody = baseUserServiceClient.getUserInfoById(userAccountResultBody.getResult().getUserId());

        checkLogin(LoginType.PASSWORD, username, () -> !BCrypt.checkpw(password, baseUserResultBody.getResult().getPassword()));
        // 获取登录token
        LoginHelper.loginByDevice(conventJbmLoginUser(baseUserResultBody.getResult()), RequestDeviceType.PC);

//        recordLogininfor(username, JbmConstants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
//        return StpUtil.getTokenValue();
        return this.accessTokenResult(StpUtil.getTokenInfo());
    }

    private AccessTokenResult accessTokenResult(SaTokenInfo tokenInfo) {
        AccessTokenResult accessTokenResult = new AccessTokenResult();
        accessTokenResult.setAccessToken(tokenInfo.getTokenValue());
        accessTokenResult.setExpiresIn(tokenInfo.getTokenActivityTimeout());
        return accessTokenResult;
    }


//    public String smsLogin(String phonenumber, String smsCode) {
//        // 通过手机号查找用户
//        JbmLoginUser userInfo = remoteUserService.getUserInfoByPhonenumber(phonenumber);
//
//        checkLogin(LoginType.SMS, userInfo.getUsername(), () -> !validateSmsCode(phonenumber, smsCode));
//        // 生成token
//        LoginHelper.loginByDevice(userInfo, RequestDeviceType.APP);
//
//        recordLogininfor(userInfo.getUsername(), JbmConstants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
//        return StpUtil.getTokenValue();
//    }
//
//    public String xcxLogin(String xcxCode) {
//        // xcxCode 为 小程序调用 wx.login 授权后获取
//        // todo 自行实现 校验 appid + appsrcret + xcxCode 调用登录凭证校验接口 获取 session_key 与 openid
//        String openid = "";
//        XcxJbmLoginUser userInfo = remoteUserService.getUserInfoByOpenid(openid);
//        // 生成token
//        LoginHelper.loginByDevice(userInfo, DeviceType.XCX);
//
//        recordLogininfor(userInfo.getUsername(), JbmConstants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
//        return StpUtil.getTokenValue();
//    }

    public void logout(String loginName) {
        StpUtil.logout();
//        recordLogininfor(loginName, JbmConstants.LOGOUT, MessageUtils.message("user.logout.success"));
    }

    /**
     * 注册
     */
    public void register(RegisterBody registerBody) {
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        // 校验用户类型是否存在
        String userType = UserType.getUserType(registerBody.getUserType()).getUserType();
        // 注册用户信息
        BaseUser sysUser = new BaseUser();
        sysUser.setUserName(username);
        sysUser.setNickName(username);
        sysUser.setPassword(SecurityUtils.encryptPassword(password));
        sysUser.setUserType(userType);
//        boolean regFlag = remoteUserService.registerUserInfo(sysUser);
//        if (!regFlag) {
//            throw new UserException("user.register.error");
//        }
//        recordLogininfor(username, JbmConstants.REGISTER, MessageUtils.message("user.register.success"));
    }

//    /**
//     * 记录登录信息
//     *
//     * @param username 用户名
//     * @param status   状态
//     * @param message  消息内容
//     * @return
//     */
//    public void recordLogininfor( String username, String status, String message) {
//        BaseAccountLogs logininfor = new BaseAccountLogs();
//        logininfor.setUserName(username);
//        logininfor.setIpaddr(ServletUtils.getClientIP());
//        logininfor.setMsg(message);
//        // 日志状态
//        if (StrUtil.equalsAny(status, JbmConstants.LOGIN_SUCCESS, JbmConstants.LOGOUT, JbmConstants.REGISTER)) {
//            logininfor.setStatus(JbmConstants.LOGIN_SUCCESS_STATUS);
//        } else if (JbmConstants.LOGIN_FAIL.equals(status)) {
//            logininfor.setStatus(JbmConstants.LOGIN_FAIL_STATUS);
//        }
//        remoteLogService.saveLogininfor(logininfor);
//    }

    /**
     * 校验短信验证码
     */
    private boolean validateSmsCode(String phonenumber, String smsCode) {
        // todo 此处使用手机号查询redis验证码与参数验证码是否一致 用户自行实现
        return true;
    }

    /**
     * 登录校验
     */
    private void checkLogin(LoginType loginType, String username, Supplier<Boolean> supplier) {
        String errorKey = JbmCacheConstants.LOGIN_ERROR + username;
        Long errorLimitTime = new Long(JbmCacheConstants.LOGIN_ERROR_LIMIT_TIME);
        Integer setErrorNumber = JbmCacheConstants.LOGIN_ERROR_NUMBER;
        String loginFail = JbmConstants.LOGIN_FAIL;

        // 获取用户登录错误次数(可自定义限制策略 例如: key + username + ip)
        Integer errorNumber = redisService.getCacheObject(errorKey);
        // 锁定时间内登录 则踢出
        if (ObjectUtil.isNotNull(errorNumber) && errorNumber.equals(setErrorNumber)) {
//            recordLogininfor(username, loginFail, MessageUtils.message(loginType.getRetryLimitExceed(), errorLimitTime));
            throw new UserException(loginType.getRetryLimitExceed(), errorLimitTime);
        }

        if (supplier.get()) {
            // 是否第一次
            errorNumber = ObjectUtil.isNull(errorNumber) ? 1 : errorNumber + 1;
            // 达到规定错误次数 则锁定登录
            if (errorNumber.equals(setErrorNumber)) {
                redisService.setCacheObject(errorKey, errorNumber, errorLimitTime, TimeUnit.MINUTES);
//                recordLogininfor(username, loginFail, MessageUtils.message(loginType.getRetryLimitExceed(), errorLimitTime));
                throw new UserException(loginType.getRetryLimitExceed(), errorLimitTime);
            } else {
                // 未达到规定错误次数 则递增
                redisService.setCacheObject(errorKey, errorNumber);
//                recordLogininfor(username, loginFail, MessageUtils.message(loginType.getRetryLimitCount(), errorNumber));
                throw new UserException(loginType.getRetryLimitCount(), errorNumber);
            }
        }
        // 登录成功 清空错误次数
        redisService.deleteObject(errorKey);
    }
}
