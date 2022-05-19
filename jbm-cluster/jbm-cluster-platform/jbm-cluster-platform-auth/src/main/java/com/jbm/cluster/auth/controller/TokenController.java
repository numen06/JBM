package com.jbm.cluster.auth.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.jbm.cluster.api.constants.RequestDeviceType;
import com.jbm.cluster.api.model.auth.AccessTokenResult;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.auth.form.LoginBody;
import com.jbm.cluster.auth.form.RegisterBody;
import com.jbm.cluster.auth.service.SysLoginService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * token 控制
 *
 * @author Lion Li
 */
@Validated
@Api(value = "认证鉴权控制器", tags = {"认证鉴权管理"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/token")
public class TokenController {

    @Autowired
    private SysLoginService sysLoginService;

    @ApiOperation("登录方法")
    @PostMapping("login")
    public ResultBody<Map<String, Object>> login(@Validated LoginBody form) {
        JbmLoginUser jbmLoginUser = new JbmLoginUser();
        jbmLoginUser.setUserId(1l);
//        jbmLoginUser.setUserType(UserType.SYS_USER.getUserType());
        jbmLoginUser.setUsername("test");
        // 获取登录token
        LoginHelper.loginByDevice(jbmLoginUser, RequestDeviceType.PC);
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        AccessTokenResult accessTokenResult = new AccessTokenResult();
        accessTokenResult.setAccessToken(tokenInfo.getTokenValue());
        accessTokenResult.setExpiresIn(tokenInfo.getTokenActivityTimeout());
        // 接口返回信息
        Map<String, Object> rspMap = BeanUtil.beanToMap(accessTokenResult, true, false);
        return ResultBody.ok().data(rspMap);
//        // 用户登录
//        AccessTokenResult accessTokenResult = sysLoginService.login(form.getUsername(), form.getPassword());
//        // 接口返回信息
//        Map<String, Object> rspMap = BeanUtil.beanToMap(accessTokenResult, true, false);
//        return ResultBody.ok().data(rspMap);
    }

    @SaCheckLogin
    @ApiOperation("获取用户信息")
    @GetMapping("userInfo")
    public ResultBody<JbmLoginUser> userInfo() {
        return ResultBody.ok().data(LoginHelper.getLoginUser());
    }

    @SaCheckRole("test")
    @ApiOperation("获取角色信息")
    @GetMapping("role")
    public ResultBody<JbmLoginUser> testRole() {
        return ResultBody.ok().data(LoginHelper.getLoginUser());
    }

//    /**
//     * 短信登录(示例)
//     *
//     * @param smsLoginBody 登录信息
//     * @return 结果
//     */
//    @ApiOperation("短信登录(示例)")
//    @PostMapping("/smsLogin")
//    public ResultBody<Map<String, Object>> smsLogin(@Validated @RequestBody SmsLoginBody smsLoginBody) {
//        Map<String, Object> ajax = new HashMap<>();
//        // 生成令牌
//        String token = sysLoginService.smsLogin(smsLoginBody.getPhonenumber(), smsLoginBody.getSmsCode());
//        ajax.put(Constants.TOKEN, token);
//        return ResultBody.ok().data(ajax);
//    }

//    /**
//     * 小程序登录(示例)
//     *
//     * @param xcxCode 小程序code
//     * @return 结果
//     */
//    @ApiOperation("小程序登录(示例)")
//    @PostMapping("/xcxLogin")
//    public ResultBody<Map<String, Object>> xcxLogin(@NotBlank(message = "{xcx.code.not.blank}") String xcxCode) {
//        Map<String, Object> ajax = new HashMap<>();
//        // 生成令牌
//        String token = sysLoginService.xcxLogin(xcxCode);
//        ajax.put(Constants.TOKEN, token);
//        return ResultBody.ok().data(ajax);
//    }

    @ApiOperation("登出方法")
    @DeleteMapping("logout")
    public ResultBody<Void> logout() {
        try {
            sysLoginService.logout(LoginHelper.getUsername());
        } catch (NotLoginException e) {
            return ResultBody.failed().msg("还没有登录");
        }
        return ResultBody.ok();
    }

    @ApiOperation("用户注册")
    @PostMapping("register")
    public ResultBody<Void> register(@RequestBody RegisterBody registerBody) {
        // 用户注册
        sysLoginService.register(registerBody);
        return ResultBody.ok();
    }

}
