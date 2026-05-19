package jbm.framework.boot.autoconfigure.extendfield.advice;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;
import java.util.Map;

/**
 * 将响应中的 extendData 平铺到实体同层。
 */
@ControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "jbm.extend-field", name = {"enabled", "auto-flatten"}, havingValue = "true")
public class ResultExtendAop implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (!(body instanceof ResultBody)) {
            return body;
        }
        ResultBody<?> resultBody = (ResultBody<?>) body;
        Object result = resultBody.getResult();
        if (result == null) {
            return body;
        }
        if (result instanceof DataPaging) {
            DataPaging paging = (DataPaging) result;
            List contents = paging.getContents();
            if (contents != null) {
                for (int i = 0; i < contents.size(); i++) {
                    contents.set(i, flattenObject(contents.get(i)));
                }
            }
        } else if (result instanceof List) {
            List list = (List) result;
            for (int i = 0; i < list.size(); i++) {
                list.set(i, flattenObject(list.get(i)));
            }
        } else {
            @SuppressWarnings("unchecked")
            ResultBody<Object> raw = (ResultBody<Object>) resultBody;
            raw.setResult(flattenObject(result));
        }
        return body;
    }

    private Object flattenObject(Object entity) {
        if (entity == null) {
            return null;
        }
        if (entity instanceof Boolean || entity instanceof Number || entity instanceof CharSequence) {
            return entity;
        }
        JSONObject json = JSON.parseObject(JSON.toJSONString(entity));
        if (json == null || !json.containsKey("extendData")) {
            return entity;
        }
        flattenExtendData(json);
        return json;
    }

    static void flattenExtendData(JSONObject entity) {
        if (entity == null || !entity.containsKey("extendData")) {
            return;
        }
        Object extendData = entity.get("extendData");
        if (extendData == null) {
            entity.remove("extendData");
            return;
        }
        JSONObject extendJson;
        if (extendData instanceof JSONObject) {
            extendJson = (JSONObject) extendData;
        } else if (extendData instanceof Map) {
            extendJson = new JSONObject((Map<String, Object>) extendData);
        } else if (extendData instanceof String && StrUtil.isNotBlank((String) extendData)) {
            extendJson = JSON.parseObject((String) extendData);
        } else {
            entity.remove("extendData");
            return;
        }
        if (extendJson == null || extendJson.isEmpty()) {
            entity.remove("extendData");
            return;
        }
        entity.remove("extendData");
        entity.remove("extendQuery");
        entity.putAll(extendJson);
    }
}
