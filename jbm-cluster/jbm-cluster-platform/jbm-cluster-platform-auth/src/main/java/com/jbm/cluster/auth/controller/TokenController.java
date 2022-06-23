package com.jbm.cluster.auth.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.auth.service.SysLoginService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


}
