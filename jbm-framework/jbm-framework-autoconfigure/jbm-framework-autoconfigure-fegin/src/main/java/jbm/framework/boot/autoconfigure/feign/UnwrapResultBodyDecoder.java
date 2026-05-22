package jbm.framework.boot.autoconfigure.feign;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import feign.Response;
import feign.Util;
import feign.codec.Decoder;
import com.jbm.framework.metadata.bean.ResultBody;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class UnwrapResultBodyDecoder implements Decoder {
    private final Decoder delegate;

    public UnwrapResultBodyDecoder(Decoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException {
        if (type instanceof Class && ResultBody.class.isAssignableFrom((Class<?>) type)) {
            return delegate.decode(response, type);
        }
        if (response.body() == null) {
            return null;
        }
        byte[] body = Util.toByteArray(response.body().asInputStream());
        if (body.length == 0) {
            return null;
        }
        String json = new String(body, StandardCharsets.UTF_8);
        JSONObject root = JSON.parseObject(json);
        if (root == null || !root.containsKey("success")) {
            return delegate.decode(response.toBuilder().body(body).build(), type);
        }
        if (Boolean.FALSE.equals(root.getBoolean("success"))) {
            throw new RemoteServiceException(root.getInteger("code"), root.getString("message"), response.request().url());
        }
        Object result = root.get("result");
        if (result == null) {
            return null;
        }
        if (type instanceof Class) {
            return JSON.parseObject(JSON.toJSONString(result), (Class<?>) type);
        }
        return JSON.parseObject(JSON.toJSONString(result), type);
    }
}
