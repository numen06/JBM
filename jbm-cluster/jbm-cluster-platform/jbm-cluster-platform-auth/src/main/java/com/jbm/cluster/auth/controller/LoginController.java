package com.jbm.cluster.auth.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import com.jbm.cluster.api.model.auth.AccessTokenResult;
import com.jbm.cluster.auth.service.SysLoginService;
import com.jbm.cluster.auth.service.feign.BaseUserServiceClient;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @Created wesley.zhang
 * @Date 2022/5/4 13:36
 * @Description TODO
 */
//@Api(tags = "账号认证中心")
//@RestController
//@RequestMapping("/login")
public class LoginController {

    @Autowired(required = false)
    private BaseUserServiceClient baseUserServiceClient;

    @Autowired
    private SysLoginService sysLoginService;

    /**
     * 获取用户基础信
     *
     * @return
     */
    @ApiOperation(value = "注册帐号", notes = "")
    @PostMapping("/register")
    public ResultBody register(HttpServletRequest request, @RequestParam(value = "vcode") String vcode,
                               @RequestParam(value = "userName") String userName,
                               @RequestParam(value = "nickName", required = false) String nickName,
                               @RequestParam(value = "accountType", required = false) String accountType,
                               @RequestParam(value = "password") String password,
                               @RequestParam(value = "confirmPassword") String confirmPassword) {
//        if (!vCoderService.verify(vcode, null)) {
//            return ResultBody.failed().msg("验证码错误");
//        }
        String registerIp = ServletUtil.getClientIP(request, null);
        return baseUserServiceClient.register(registerIp, userName, nickName, accountType, password, confirmPassword);
    }


    /**
     * 获取用户访问令牌
     * 基于oauth2密码模式登录
     *
     * @param username
     * @param password
     * @return access_token
     */
    @ApiOperation(value = "登录获取用户访问令牌", notes = "基于oauth2密码模式登录,无需签名,返回access_token")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", required = true, value = "登录名", paramType = "form"),
            @ApiImplicitParam(name = "password", required = true, value = "登录密码", paramType = "form"),
            @ApiImplicitParam(name = "type", required = false, value = "登录类型", paramType = "form"),
            @ApiImplicitParam(name = "vcode", required = false, value = "验证码", paramType = "form")
    })
    @PostMapping("/token")
    public Object getLoginToken(@RequestParam String username, @RequestParam String password, @RequestParam(required = false) String type, @RequestHeader HttpHeaders httpHeaders, HttpServletRequest request) throws Exception {
        AccessTokenResult accessTokenResult = sysLoginService.login(username, password);
        // 接口返回信息
        Map<String, Object> rspMap = BeanUtil.beanToMap(accessTokenResult, true, false);
        if (ObjectUtil.isNotEmpty(accessTokenResult)) {
            return ResultBody.ok().data(accessTokenResult);
        } else {
            return accessTokenResult;
        }
    }

    /**
     * 退出移除令牌
     */
    @ApiOperation(value = "退出并移除令牌", notes = "退出并移除令牌,令牌将失效")
    @GetMapping("/token/logout")
    public ResultBody removeToken() {
        String token = LoginHelper.getUsername();
        if (StrUtil.isBlank(token))
            ResultBody.failed().msg("令牌异常");
        sysLoginService.logout(token);
        return ResultBody.ok().msg("退出成功");
    }
}
