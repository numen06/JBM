package jbm.framework.boot.autoconfigure.extendfield.advice;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jbm.framework.boot.autoconfigure.extendfield.service.FieldDefinitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 请求体扩展字段处理：写入时按 formCode 拆分字段；查询时将 extend 对象映射为 extendQuery。
 */
@Slf4j
@ControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "jbm.extend-field", name = "enabled", havingValue = "true")
public class ExtendFieldRequestBodyAdvice extends RequestBodyAdviceAdapter {

    @Autowired(required = false)
    private FieldDefinitionService fieldDefinitionService;

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                          Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
                                           Type targetType, Class<? extends HttpMessageConverter<?>> converterType)
            throws IOException {
        byte[] raw = StreamUtils.copyToByteArray(inputMessage.getBody());
        if (raw.length == 0) {
            return new FixedBodyInputMessage(raw, inputMessage.getHeaders());
        }
        JSONObject json;
        try {
            json = JSON.parseObject(new String(raw, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.debug("extend field advice skip: not json body");
            return new FixedBodyInputMessage(raw, inputMessage.getHeaders());
        }
        if (json == null) {
            return new FixedBodyInputMessage(raw, inputMessage.getHeaders());
        }

        if (json.containsKey("formCode") && fieldDefinitionService != null) {
            String formCode = json.getString("formCode");
            json.remove("formCode");
            Set<String> fieldCodes = fieldDefinitionService.getExtendFieldNames(formCode);
            if (fieldCodes != null && !fieldCodes.isEmpty()) {
                JSONObject extendData = new JSONObject();
                for (String key : new ArrayList<>(json.keySet())) {
                    if (fieldCodes.contains(key)) {
                        extendData.put(key, json.remove(key));
                    }
                }
                if (!extendData.isEmpty()) {
                    json.put("extendData", extendData);
                }
            }
        }

        Object extendObj = json.get("extend");
        if (extendObj instanceof JSONObject) {
            json.put("extendQuery", extendObj);
        }

        byte[] body = json.toJSONString().getBytes(StandardCharsets.UTF_8);
        return new FixedBodyInputMessage(body, inputMessage.getHeaders());
    }

    private static final class FixedBodyInputMessage implements HttpInputMessage {
        private final byte[] body;
        private final HttpHeaders headers;

        private FixedBodyInputMessage(byte[] body, HttpHeaders headers) {
            this.body = body;
            this.headers = headers;
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}
