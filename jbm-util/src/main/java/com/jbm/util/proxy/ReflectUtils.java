package com.jbm.util.proxy;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.exceptions.UtilException;
import cn.hutool.core.lang.Filter;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.TypeUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jbm.util.proxy.annotation.RequestBody;
import com.jbm.util.proxy.annotation.RequestHeader;
import com.jbm.util.proxy.annotation.RequestParam;
import com.jbm.util.proxy.wapper.RequestHeaders;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * 通过JSON字符串内容执行方法
     *
     * @param obj
     * @param method
     * @param jsonStr
     * @param <T>
     * @return
     * @throws UtilException
     */
//    public static <T> T invokeMethodFromJsonData(Object obj, Method method, String jsonStr) throws UtilException {
//        //没有参数
//        if (method.getParameterCount() == 0) {
//            return ReflectUtil.invoke(obj, method);
//        }
//        //有一个参数
//        else if (method.getParameterCount() == 1) {
//            Parameter[] parameters = method.getParameters();
//            Class<?> pType = parameters[0].getType();
//            Object pObject = null;
//            if (String.class.equals(pType)) {
//                return ReflectUtil.invoke(obj, method, jsonStr);
//            } else {
//                pObject = JSON.parseObject(jsonStr, pType);
//            }
//            return ReflectUtil.invoke(obj, method, pObject);
//        }
//        //有多个参数
//        else if (method.getParameterCount() > 1) {
//            Parameter[] parameters = method.getParameters();
//            Map<String, Object> pMap = new HashMap();
//            for (Parameter p : parameters) {
//                Class<?> pType = p.getType();
//                JSONObject jsonObject = JSON.parseObject(jsonStr);
//                Object pObject = jsonObject.getObject(p.getName(), pType);
//                pMap.put(p.getName(), pObject);
//            }
//            return ReflectUtil.invokeWithCheck(obj, method, pMap.values().toArray());
//        }
//        return null;
//    }
    public static <T> T invokeMethodFromJsonData(Object obj, Method method, String jsonStr, RequestHeaders requestHeaders) throws UtilException {
        //没有参数
        if (method.getParameterCount() == 0) {
            return ReflectUtil.invoke(obj, method);
        }
        Object jsonObject = JSON.parse(jsonStr);
        Object[] params = new Object[method.getParameterCount()];
        Parameter[] paramTypes = method.getParameters();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        List<Integer> jumpIndex = new ArrayList<>();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            for (Annotation annotation : annotations) {//获取注解名
                if (annotation.annotationType().isAssignableFrom(RequestBody.class)) {
                    Object pObject = null;
                    if (jsonObject instanceof JSONObject) {
                        pObject = JSON.parseObject(jsonStr, paramTypes[i].getType());
                    }
                    if (jsonObject instanceof JSONArray) {
                        Type type = TypeUtil.getParamType(method, i);
                        Type type2 = TypeUtil.getTypeArgument(type);
                        pObject = JSON.parseArray(jsonStr, TypeUtil.getClass(type2));
                    }
                    params[i] = pObject;
                    jumpIndex.add(i);
                }
                if (annotation.annotationType().isAssignableFrom(RequestParam.class)) {
                    String name = ((RequestParam) annotation).value();
                    Object pObject = ((JSONObject) jsonObject).getObject(name, paramTypes[i].getType());
                    params[i] = pObject;
                    jumpIndex.add(i);
                }
                if (annotation.annotationType().isAssignableFrom(RequestHeader.class)) {
                    String name = ((RequestHeader) annotation).value();
                    Object pObject = requestHeaders.get(name, paramTypes[i].getType());
                    params[i] = pObject;
                    jumpIndex.add(i);
                }
            }
        }
        for (int i = 0; i < paramTypes.length; i++) {
            if (jumpIndex.contains(i))
                continue;
            Parameter p = paramTypes[i];
            Class<?> pz = p.getType();
            if (pz.isAssignableFrom(RequestHeaders.class)) {
                params[i] = requestHeaders;
            }
        }
        return ReflectUtil.invokeWithCheck(obj, method, params);
    }


}
