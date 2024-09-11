package com.jbm.util.key;

import com.alibaba.fastjson.JSONArray;
import lombok.Getter;

import java.util.Arrays;

public class KeyArray {

    @Getter
    private final JSONArray data = new JSONArray();

    public KeyArray() {
    }

    public KeyArray(JSONArray array) {
        data.addAll(Arrays.asList(array.toArray()));
    }

    public KeyArray(Object... keys) {
        data.addAll(java.util.Arrays.asList(keys));
    }

    public KeyArray of(Object... keys) {
        data.addAll(java.util.Arrays.asList(keys));
        return this;
    }

    public static KeyArray from(String json) {
        return new KeyArray(JSONArray.parseArray(json));
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
