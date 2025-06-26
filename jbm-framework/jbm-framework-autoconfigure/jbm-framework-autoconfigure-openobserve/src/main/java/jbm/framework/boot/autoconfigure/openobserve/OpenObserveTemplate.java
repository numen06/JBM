package jbm.framework.boot.autoconfigure.openobserve;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jbm.framework.exceptions.ServiceException;
import lombok.extern.slf4j.Slf4j;

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


    public QueryResult selectLogs(QueryBean queryBean) {
        String url = StrUtil.format("{}/api/{}/{}/_search", openObserveProperties.getBaseUrl(), openObserveProperties.getOrganization(), openObserveProperties.getStream());
        HttpRequest request = HttpRequest.of(url).basicAuth(openObserveProperties.getUsername(), openObserveProperties.getPassword()).method(Method.POST);
        request.contentType("application/json");
        JSONObject queryBeanJson = new JSONObject();
        queryBeanJson.put("query", queryBean);
        request.body(JSON.toJSONString(queryBeanJson));
        HttpResponse response = request.execute();
        if (response.getStatus() != HttpStatus.HTTP_OK) {
            log.error("错误信息:{}", response.body());
            throw ServiceException.of(response.body());
        }
        String body = response.body();
        return JSON.parseObject(body, QueryResult.class);
    }
}
