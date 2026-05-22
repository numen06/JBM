package com.jbm.cluster.common.feign;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import jbm.framework.boot.autoconfigure.feign.RemoteServiceException;

import java.nio.charset.StandardCharsets;

public class FeignClientErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            if (response.body() != null) {
                String json = new String(Util.toByteArray(response.body().asInputStream()), StandardCharsets.UTF_8);
                JSONObject root = JSON.parseObject(json);
                if (root != null && root.containsKey("success")) {
                    return new RemoteServiceException(root.getInteger("code"), root.getString("message"), response.request().url());
                }
            }
        } catch (Exception ignored) {
        }
        return defaultDecoder.decode(methodKey, response);
    }
}