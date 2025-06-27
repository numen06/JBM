package jbm.framework.boot.autoconfigure.openobserve;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jbm.framework.exceptions.ServiceException;
import jbm.framework.boot.autoconfigure.openobserve.model.QueryBean;
import lombok.extern.slf4j.Slf4j;

import java.net.HttpCookie;

@Slf4j
public class OpenObserveTemplate {

    private final OpenObserveProperties openObserveProperties;

    public OpenObserveTemplate(OpenObserveProperties openObserveProperties) {
        this.openObserveProperties = openObserveProperties;
    }

    public void postLogs(Object... logs) {
        if (logs.length == 1) {
            Object log = logs[0];
            if (log instanceof String) {
                this.postLogStr((String) log);
                return;
            }
        }
        this.postLogStr(JSON.toJSONString(logs));
    }

    private String auth_tokens = null;

    public void login() {
        String url = StrUtil.format("{}/auth/login", openObserveProperties.getBaseUrl());
        HttpRequest request = HttpRequest.of(url).method(Method.POST);
        request.contentType("application/json");
        JSONObject loginInfo = new JSONObject();
        loginInfo.put("name", openObserveProperties.getUsername());
        loginInfo.put("password", openObserveProperties.getPassword());
        request.body(JSON.toJSONString(loginInfo));
        HttpResponse response = request.execute();
        if (response.getStatus() != HttpStatus.HTTP_OK) {
            throw ServiceException.of("登录错误");
        } else {
            String body = response.body();
            JSONObject jsonObject = JSON.parseObject(body);
            boolean status = jsonObject.getBoolean("status");
            if (!status) {
                throw ServiceException.of("认证失败");
            }
        }

        auth_tokens = response.getCookieValue("auth_tokens");
    }

    public void postLogStr(String logStr) {
        String firstChar = StrUtil.sub(logStr, 0, 1);
        StringBuilder sb = new StringBuilder(logStr);
        //如果不是数组则组成数组
        if (firstChar.equals("{")) {
            sb.insert(0, "[");
            sb.append("]");
        }
        String url = StrUtil.format("{}/api/{}/{}/_json", openObserveProperties.getBaseUrl(), openObserveProperties.getOrganization(), openObserveProperties.getStream());
        HttpRequest request = HttpRequest.of(url).basicAuth(openObserveProperties.getUsername(), openObserveProperties.getPassword()).method(Method.POST);
        request.contentType("application/json");
        request.body(sb.toString());
        HttpResponse response = request.execute();
        if (response.getStatus() != HttpStatus.HTTP_OK) {
            log.error("错误信息:{}", response.body());
            throw ServiceException.of(response.body());
        }
        log.info("发送成功:{}", response.body());
    }

    public HttpRequest getRequest(String url) {
        return getRequest(url, Method.POST, ContentType.JSON);
    }

    public HttpRequest getRequest(String url, Method method, ContentType contentType) {
        HttpRequest request = HttpRequest.of(url);
        request.method(method);
        request.contentType(contentType.getValue());
        if (auth_tokens == null) {
            login();
        }
        request.cookie(new HttpCookie("auth_tokens", auth_tokens));
        return request;
    }

    public QueryResult selectLogs(QueryBean queryBean) {
        String url = StrUtil.format("{}/api/{}/_search", openObserveProperties.getBaseUrl(), openObserveProperties.getOrganization());
        HttpRequest request = getRequest(url);
//        JSONObject queryBeanJson = new JSONObject();
//        queryBeanJson.put("query", queryBean);
        String requestBody = JSON.toJSONString(queryBean);
        request.body(requestBody);
        HttpResponse response = request.execute();
        if (response.getStatus() != HttpStatus.HTTP_OK) {
            log.error("请求信息:{}",requestBody);
            log.error("错误信息:{}", response.body());
            throw ServiceException.of(response.body());
        }
        String body = response.body();
        return JSON.parseObject(body, QueryResult.class);
    }
}
