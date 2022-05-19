package com.jbm.cluster.auth.controller;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Consts;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Handle;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Util;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Created wesley.zhang
 * @Date 2022/5/15 10:18
 * @Description TODO
 */
@RestController
public class OAuth2ServerController {

    // 处理所有OAuth相关请求
    @RequestMapping("/oauth2/*")
    public Object oauth2() {
        System.out.println("------- 进入请求: " + SaHolder.getRequest().getUrl());
        Object result = SaOAuth2Handle.serverRequest();
        if (SaOAuth2Consts.NOT_HANDLE.equals(result)) {
            return ResultBody.failed().httpStatus(400).code(400).msg("输入参数错误,没有找到匹配的授权模式");
        }
        return result;
    }

    // 处理所有OAuth相关请求
    @RequestMapping("/oauth/*")
    public Object oauth() {
        return this.oauth2();
    }


    // ---------- 开放相关资源接口： Client端根据 Access-Token ，置换相关资源 ------------
    // 获取Userinfo信息：昵称、头像、性别等等
    @RequestMapping("/oauth2/userinfo")
    public ResultBody<JbmLoginUser> userinfo() {
        if (StpUtil.isLogin()) {
            return ResultBody.ok().data(LoginHelper.getLoginUser());
        }
        // 获取 Access-Token 对应的账号id
        String accessToken = SaHolder.getRequest().getParam("access_token");
        if (StrUtil.isNotBlank(accessToken)) {
            Object loginId = SaOAuth2Util.getLoginIdByAccessToken(accessToken);
            System.out.println("-------- 此Access-Token对应的账号id: " + loginId);
            return ResultBody.ok().data(LoginHelper.getLoginUser(accessToken));
        }
        return ResultBody.error("认证失败,无法获取用户信息");
    }

}
