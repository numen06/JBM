package com.jbm.cluster.auth.service;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.oauth2.config.SaOAuth2Config;
import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.constants.RequestDeviceType;
import com.jbm.cluster.api.entitys.basic.BaseAccount;
import com.jbm.cluster.api.entitys.basic.BaseAccountLogs;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.auth.LoginWay;
import com.jbm.cluster.api.form.auth.RegisterForm;
import com.jbm.cluster.api.model.auth.AccessTokenResult;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.api.service.ILoginAuthenticate;
import com.jbm.cluster.auth.service.feign.BaseUserServiceClient;
import com.jbm.cluster.auth.service.feign.DynamicLoginFeignClient;
import com.jbm.cluster.common.basic.JbmClusterStreamTemplate;
import com.jbm.cluster.common.basic.utils.IpUtils;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.core.constant.JbmCacheConstants;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.exceptions.user.UserException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.WebUtils;
import jbm.framework.boot.autoconfigure.redis.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 登录校验方法
 *
 * @author wesley.zhang
 */
@Slf4j
@Service
public class SysLoginService {
    @Autowired(required = false)
    private BaseUserServiceClient baseUserServiceClient;
    //    @DubboReference
//    private RemoteLogService remoteLogService;
    //    @DubboReference
//    private RemoteUserService remoteUserService;
    @Autowired
    private JbmClusterStreamTemplate jbmClusterStreamTemplate;

    @Autowired
    private RedisService redisService;

    public JbmLoginUser conventJbmLoginUser(BaseUser baseUser) {
        JbmLoginUser jbmLoginUser = new JbmLoginUser();
        BeanUtil.copyProperties(baseUser, jbmLoginUser);
        return jbmLoginUser;
    }

    // Sa-OAuth2 定制化配置
    @Autowired
    public void setSaOAuth2Config(SaOAuth2Config cfg) {
        cfg.
                // 未登录的视图
                        setNotLoginView(() -> {
                    return SaResult.error("你还没有登录");
                }).
                // 登录处理函数
                        setDoLoginHandle((username, password) -> {
                    String loginTypeValue = SaHolder.getRequest().getParam("loginType");
                    LoginType loginType = LoginType.PASSWORD;
                    if (StrUtil.isNotEmpty(loginTypeValue)) {
                        loginType = LoginType.valueOf(loginTypeValue.toUpperCase());
                    }
                    ResultBody<JbmLoginUser> resultBody = this.login(username, password, loginType);
                    //如果获取成功则直接登录，失败则返回错误信息
                    if (resultBody.getSuccess()) {
                        log.info("获取到了用户信息,触发登录");
                        checkLogin(loginType, username, () -> true);
                        LoginHelper.login(resultBody.getResult());
                        recordLogininfor(resultBody.getResult(), true, null);
                    } else {
                        checkLogin(loginType, username, () -> false);
                        recordLogininfor(resultBody.getResult(), false, resultBody.getMessage());
                    }
                    return resultBody;
                }).
                // 授权确认视图
                        setConfirmView((clientId, scope) -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("clientId", clientId);
                    map.put("scope", scope);
                    return SaResult.error("确认登录");
                })
        ;
    }

    @Autowired
    private DynamicLoginFeignClient dynamicLoginFeignClient;

    public ResultBody<JbmLoginUser> login(String username, String password, LoginType loginType) {
        ILoginAuthenticate ILoginAuthenticate = dynamicLoginFeignClient.getFeginLoginAuthenticate(loginType);
        //将密码加密传入服务
//        String encryptPassword = LoginHelper.encryptPassword(password);
        return ILoginAuthenticate.login(username, password, loginType.toString());
    }

    /**
     * 登录
     */
    public AccessTokenResult login(String username, String password) {
        ResultBody<UserAccount> userAccountResultBody = baseUserServiceClient.userLogin(username);
        ResultBody<BaseUser> baseUserResultBody = baseUserServiceClient.getUserInfoById(userAccountResultBody.getResult().getUserId());

        checkLogin(LoginType.PASSWORD, username, () -> !BCrypt.checkpw(password, baseUserResultBody.getResult().getPassword()));
        // 获取登录token
        LoginHelper.loginByDevice(conventJbmLoginUser(baseUserResultBody.getResult()), RequestDeviceType.PC.getDevice());

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

    public void logout(Object loginId) {
        LoginHelper.loginout();
//        recordLogininfor(loginName, JbmConstants.LOGOUT, MessageUtils.message("user.logout.success"));
    }

    /**
     * 注册
     */
    public void register(RegisterForm registerBody) {
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        // 校验用户类型是否存在
//        String userType = UserType.getUserType(registerBody.getUserType()).getUserType();
        // 注册用户信息
        BaseUser sysUser = new BaseUser();
        sysUser.setUserName(username);
        sysUser.setNickName(registerBody.getNickName());
        sysUser.setPassword(LoginHelper.encryptPassword(password));
//        sysUser.setUserType(userType);
//        boolean regFlag = remoteUserService.registerUserInfo(sysUser);
//        if (!regFlag) {
//            throw new UserException("user.register.error");
//        }
//        recordLogininfor(username, JbmConstants.REGISTER, MessageUtils.message("user.register.success"));
    }

    /**
     * 记录登录信息
     *
     * @param message 消息内容
     * @return
     */
    public void recordLogininfor(JbmLoginUser jbmLoginUser, Boolean loginStatus, String message) {
        HttpServletRequest request = WebUtils.getHttpServletRequest();
        BaseAccountLogs log = new BaseAccountLogs();
        log.setDomain(jbmLoginUser.getUserType());
        log.setUserId(jbmLoginUser.getUserId());
        log.setAccount(jbmLoginUser.getAccount());
//        log.setAccountId(StrUtil.toString(baseAccount.getAccountId()));
        log.setAccountType(jbmLoginUser.getAccountType());
        log.setLoginIp(IpUtils.getRequestIp(request));
        log.setLoginAgent(request.getHeader(HttpHeaders.USER_AGENT));
        UserAgent userAgent = UserAgentUtil.parse(log.getLoginAgent());
        log.setBrowser(userAgent.getBrowser().getName() + " " + userAgent.getVersion());
        log.setOs(userAgent.getOs().getName());
        log.setLoginStatus(loginStatus);
        jbmClusterStreamTemplate.sendAccountLogs(log);
    }

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

        if (!supplier.get()) {
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
