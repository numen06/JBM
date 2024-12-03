package com.jbm.util.key;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author wesley
 */
public class KeyObject implements Serializable, IKey {

    @Getter
    private final JSONObject data = new JSONObject();


    public KeyObject() {
    }

    public KeyObject(Supplier<Map<String, Object>> map) {
        this.of(map.get());
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
        return this.data.toJSONString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return this.hashCode() == obj.hashCode();
    }
}
