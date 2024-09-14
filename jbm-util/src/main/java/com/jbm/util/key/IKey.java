package com.jbm.util.key;

import com.alibaba.fastjson.JSONObject;

import java.util.Map;

public interface IKey {


    static IKey from(String json) {
        return new KeyObject(JSONObject.parseObject(json));
    }


    IKey of(Object obj);

    IKey of(Map<String, Object> map);

    IKey of(String key, Object value);
}
