package com.jbm.util.proxy;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.exceptions.UtilException;
import cn.hutool.core.lang.Filter;
import cn.hutool.core.util.ReflectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReflectUtils {


    /**
     * 查找类中包含注解的方法
     *
     * @param clazz
     * @param annotationType
     * @return
     */
    public static List<Method> findAnnotationMethods(Class<?> clazz, Class<? extends Annotation> annotationType) {
        List<Method> methods = ReflectUtil.getPublicMethods(clazz, new Filter<Method>() {
            @Override
            public boolean accept(Method method) {
                return AnnotationUtil.hasAnnotation(method, annotationType);
            }
        });
        return methods;
    }

    public static <T> T invokeMethodFromJsonData(Object obj, Method method, String jsonStr) throws UtilException {
        //有一个参数
        if (method.getParameterCount() == 1) {
            Parameter[] parameters = method.getParameters();
            Class<?> pType = parameters[0].getType();
            Object pObject = JSON.parseObject(jsonStr, pType);
            return ReflectUtil.invoke(obj, method, pObject);
        }
        //有多个参数
        else if (method.getParameterCount() > 1) {
            Parameter[] parameters = method.getParameters();
            Map<String, Object> pMap = new HashMap();
            for (Parameter p : parameters) {
                Class<?> pType = p.getType();
                JSONObject jsonObject = JSON.parseObject(jsonStr);
                Object pObject = jsonObject.getObject(p.getName(), pType);
                pMap.put(p.getName(), pObject);
            }
            return ReflectUtil.invoke(obj, method, pMap.values().toArray());
        }
        return null;
    }


}
