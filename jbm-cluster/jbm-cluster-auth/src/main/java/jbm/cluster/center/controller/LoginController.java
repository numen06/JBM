package jbm.cluster.center.controller;

import com.alibaba.fastjson.JSONObject;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.cluster.common.security.OpenHelper;
import com.jbm.cluster.common.security.oauth2.client.JbmOAuth2ClientDetails;
import com.jbm.cluster.common.security.oauth2.client.JbmOAuth2ClientProperties;
import com.jbm.framework.mvc.WebUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;
import java.util.Map;

/**
 * @author: wesley.zhang
 * @date: 2018/11/9 15:43
 * @description:
 */
@Api(tags = "用户认证中心")
@RestController
public class LoginController {
    @Autowired
    private JbmOAuth2ClientProperties clientProperties;
    @Autowired
    private TokenStore tokenStore;
    @Autowired
    private RestTemplate restTemplate;
    /**
     * 获取用户基础信息
     *
     * @return
     */
    @ApiOperation(value = "获取当前登录用户信息", notes = "获取当前登录用户信息")
    @GetMapping("/current/user")
    public ResultBody getUserProfile() {
        return ResultBody.ok().data(OpenHelper.getUser());
    }


    /**
     * 获取当前登录用户信息-SSO单点登录
     *
     * @param principal
     * @return
     */
    @ApiOperation(value = "获取当前登录用户信息-SSO单点登录", notes = "获取当前登录用户信息-SSO单点登录")
    @GetMapping("/current/user/sso")
    public Principal principal(Principal principal) {
        return principal;
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
            @ApiImplicitParam(name = "password", required = true, value = "登录密码", paramType = "form")
    })
    @PostMapping("/login/token")
    public Object getLoginToken(@RequestParam String username, @RequestParam String password, @RequestHeader HttpHeaders httpHeaders) throws Exception {
        Map result = getToken(username, password, null,httpHeaders);
        if (result.containsKey("access_token")) {
            return ResultBody.ok().data(result);
        } else {
            return result;
        }
    }


    /**
     * 退出移除令牌
     *
     * @param token
     */
    @ApiOperation(value = "退出并移除令牌", notes = "退出并移除令牌,令牌将失效")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "token", required = true, value = "访问令牌", paramType = "form")
    })
    @PostMapping("/logout/token")
    public ResultBody removeToken(@RequestParam String token) {
        tokenStore.removeAccessToken(tokenStore.readAccessToken(token));
        return ResultBody.ok();
    }


    public JSONObject getToken(String userName, String password, String type, HttpHeaders headers) {
        JbmOAuth2ClientDetails clientDetails =  clientProperties.getOauth2().get("admin");
        String url = WebUtils.getServerUrl(WebUtils.getHttpServletRequest()) + "/oauth/token";
        // 使用oauth2密码模式登录.
        MultiValueMap<String, Object> postParameters = new LinkedMultiValueMap<>();
        postParameters.add("username", userName);
        postParameters.add("password", password);
        postParameters.add("client_id", clientDetails.getClientId());
        postParameters.add("client_secret", clientDetails.getClientSecret());
        postParameters.add("grant_type", "password");
        // 添加参数区分,第三方登录
        postParameters.add("login_type", type);
        // 使用客户端的请求头,发起请求
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // 强制移除 原来的请求头,防止token失效
        headers.remove(HttpHeaders.AUTHORIZATION);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity(postParameters, headers);
        JSONObject result = restTemplate.postForObject(url, request, JSONObject.class);
        return result;
    }
}
