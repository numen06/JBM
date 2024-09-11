package com.jbm.util.key;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;

import java.io.Serializable;
import java.util.Map;

/**
 * @author wesley
 */
public class KeyObject implements Serializable {

    @Getter
    private final JSONObject data = new JSONObject();

    public KeyObject() {
    }

    public KeyObject(String key, Object value) {
        data.put(key, value);
    }

    public KeyObject(Map<String, Object> map) {
        data.putAll(map);
    }

    public static KeyObject from(String json) {
        return new KeyObject(JSONObject.parseObject(json));
    }

    public KeyObject of(String key, Object value) {
        data.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        return data.toJSONString();
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.toString().equals(obj.toString());
    }
}
