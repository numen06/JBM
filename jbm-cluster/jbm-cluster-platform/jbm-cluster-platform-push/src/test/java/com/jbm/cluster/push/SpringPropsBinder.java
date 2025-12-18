package com.jbm.cluster.push;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class SpringPropsBinder {


    public static void bind(String str, Object obj) throws IOException {
        Properties props = new Properties();
        props.load(new StringReader(str));
        bind(props, obj);
    }

    /**
     * 将 Properties（支持 kebab-case / snake_case / dot-notation）绑定到 Bean，完全对齐 Spring 风格
     */
    public static void bind(Properties props, Object obj) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : props.stringPropertyNames()) {
            // ✅ Spring 兼容：统一转 camelCase（自动处理 - _ . 空格等）
            String camelKey =  StrUtil.replace(key, "-", "_");
//            String camelKey = StrUtil.toCamelCase(key);
            map.put(camelKey, props.getProperty(key));
        }
        BeanUtil.fillBeanWithMap(map, obj, true, true);
    }

}