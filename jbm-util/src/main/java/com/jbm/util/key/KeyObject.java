package com.jbm.util.key;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONObject;

import java.io.Serializable;
import java.util.Map;

/**
 * @author wesley
 */
public class KeyObject implements Serializable, IKey {

    private final JSONObject data = new JSONObject();

    public KeyObject() {
    }

    public KeyObject(String key, Object value) {
        this.of(key, value);
    }

    public KeyObject(Object obj) {
        this.of(obj);
    }

    public KeyObject(Map<String, Object> map) {
       this.of(map);
    }

    public <T> T to(Class<T> clazz) {
        return JSONObject.parseObject(this.data.toJSONString(), clazz);
    }

    @Override
    public IKey of(Object obj) {
        return of(BeanUtil.beanToMap(obj));
    }

    @Override
    public IKey of(Map<String, Object> map) {
        data.putAll(map);
        return this;
    }

    @Override
    public IKey of(String key, Object value) {
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
