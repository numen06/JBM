package com.jbm.cluster.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

@Controller
@Api(tags = "登录页面")
public class LoginController {

    /**
     * OAuth2 授权页面
     * 检查登录状态，未登录显示登录页，已登录显示授权确认页
     */
    @ApiOperation(value = "OAuth2授权页面")
    @GetMapping("/oauth2/authorize")
    public String authorize(
            @RequestParam(value = "response_type", defaultValue = "code", required = false) String responseType,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "scope", required = false, defaultValue = "") String scope,
            @RequestParam(value = "state", required = false) String state,
            Map<String, Object> model, HttpServletRequest request) {

        if (StrUtil.isEmpty(state)) {
            state = IdUtil.fastSimpleUUID();
        }
        //获取当前请求的URL拼接前缀
        if (StrUtil.isEmpty(redirectUri)) {
            //获取原始访问链接，拼接前缀，链接可能通过了几次转发
            String referer = request.getHeader("referer");
            if (StrUtil.isNotEmpty(referer)) {
                String url = referer;
                int index = url.indexOf("oauth2/authorize");
                if (index > 0) {
                    url = url.substring(0, index);
                }
                redirectUri = url + "/oauth2/thirdparty/local/callback";
            }else{
                String url = request.getRequestURL().toString();
                String pixel = url.substring(0, url.indexOf("oauth2/authorize"));
                redirectUri = pixel + "/oauth2/thirdparty/local/callback";
            }

        }
        if (StrUtil.isEmpty(clientId)) {
            clientId = "2zZw3p6YYtTLcOqRMpZbYrhX";
        }
        // 将参数传递给模板
        model.put("response_type", responseType);
        model.put("client_id", clientId);
        model.put("redirect_uri", redirectUri);
        model.put("scope", scope);
        model.put("state", state);

        // 检查用户是否已登录
        if (!StpUtil.isLogin()) {
            // 未登录，显示登录页面
            return "oauth2_login";
        }

        // 已登录，重定向到HTML授权页面
        return "oauth2_authorize";
    }

    /**
     * OAuth2 客户端演示页面
     */
    @ApiOperation(value = "OAuth2客户端演示页面", notes = "")
    @GetMapping("/oauth2/demo")
    public String demoPage() {
        return "oauth2_client_demo";
    }
}
