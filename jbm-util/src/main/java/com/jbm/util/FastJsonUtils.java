package com.jbm.util;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.PropertyNamingStrategy;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.jbm.util.bean.Version;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author: create by wesley
 * @date:2019/5/11
 */
@Slf4j
public class FastJsonUtils {

    private final static String st1 = "{\"username\":\"tom\",\"age\":18,\"addressTset\":[{\"provinceTest\":\"addressTset\"},{\"city\":\"上海市\"},{\"disrtictTest\":\"静安区\"}]}";
    private final static String st2 = "{username:\"tom\",age:18}";


    private static SerializeConfig webSerializeConfig;

    public static SerializeConfig defaultWebConfig() {
        if (ObjectUtil.isNotEmpty(webSerializeConfig)) {
            return webSerializeConfig;
        }
        //设置swagger ui
        SerializeConfig serializeConfig = new SerializeConfig();
        serializeConfig.put(BigInteger.class, ToStringSerializer.instance);
        serializeConfig.put(Long.class, ToStringSerializer.instance);
        serializeConfig.put(Long.TYPE, ToStringSerializer.instance);
        serializeConfig.put(Long.TYPE, ToStringSerializer.instance);
        serializeConfig.put(Version.class, ToStringSerializer.instance);
        webSerializeConfig = serializeConfig;
        return webSerializeConfig;
    }

    /**
     * 将JSON的name转换成下划线
     *
     * @param source 源文件
     * @return 转换后的字符串
     */
    public static String toSnakeCase(String source) {
        JSONObject jsonObject = JSON.parseObject(source);
        List<String> keys = getAllKey(jsonObject);
        String temp = jsonObject.toJSONString();
        for (String key : keys) {
            final String keyStr = "\"" + key + "\":";
            final String newKeyStr = "\"" + PropertyNamingStrategy.SnakeCase.translate(key) + "\":";
            if (!keyStr.equals(newKeyStr)) {
                temp = StrUtil.replace(temp, keyStr, newKeyStr);
            }
        }
        return temp;
    }

    /**
     * 将JSON的name转换成驼峰
     *
     * @param source 源文件
     * @return 转换后的字符串
     */
    public static String toUpperCamel(String source) {
        JSONObject jsonObject = JSON.parseObject(source);
        List<String> keys = getAllKey(jsonObject);
        String temp = jsonObject.toJSONString();
        for (String key : keys) {
            final String keyStr = "\"" + key + "\":";
            final String newKeyStr = "\"" + PropertyNamingStrategy.CamelCase.translate(key) + "\":";
            if (!keyStr.equals(newKeyStr)) {
                temp = StrUtil.replace(temp, keyStr, newKeyStr);
            }
        }
        return temp;
    }

    /**
     * 递归读取所有的key
     *
     * @param jsonObject
     */
    public static List<String> getAllKey(JSONObject jsonObject) {
        List<String> list = new ArrayList<>();
        Iterator<String> keys = jsonObject.keySet().iterator();// jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            list.add(key);
            if (jsonObject.get(key) instanceof JSONObject) {
                JSONObject innerObject = jsonObject.getJSONObject(key);
                list.addAll(getAllKey(innerObject));
            } else if (jsonObject.get(key) instanceof JSONArray) {
                JSONArray innerObject = jsonObject.getJSONArray(key);
                list.addAll(getAllKey(innerObject));
            }
        }
        return list;
    }

    public static List<String> getAllKey(JSONArray jsonArray) {
        List<String> list = new ArrayList<>();
        if (jsonArray != null) {
            Iterator i1 = jsonArray.iterator();
            while (i1.hasNext()) {
                Object key = i1.next();
                if (key instanceof JSONObject) {
                    JSONObject innerObject = (JSONObject) key;
                    list.addAll(getAllKey(innerObject));
                } else if (key instanceof JSONArray) {
                    JSONArray innerObject = (JSONArray) key;
                    list.addAll(getAllKey(innerObject));
                } else {
                }
            }
        }
        return list;
    }

//    public static void main(String[] args) {
//        System.out.println(st1);
//        JSONObject jsonObject1 = JSONObject.parseObject(st1);
//        List<String> stb = getAllKey(jsonObject1);
//        System.err.println(stb);
//        System.err.println(toSnakeCase(jsonObject1.toJSONString()));
//        System.err.println(toUpperCamel(jsonObject1.toJSONString()));
//    }


}
