package com.jbm.cluster.job.business.impl;

import com.alibaba.ttl.threadpool.agent.internal.javassist.*;

import com.jbm.cluster.api.entitys.job.DynamicClass;
import com.jbm.cluster.api.entitys.job.DynamicField;
import com.jbm.cluster.job.service.DynamicClassService;
import com.jbm.cluster.job.service.DynamicFieldService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author scolin
 * @description
 * @date 2025/8/5 17:28
 */
@Service
public class LoadDynamicClassService {
    @Autowired
    private DynamicClassService dynamicClassService;

    @Autowired
    private DynamicFieldService dynamicFieldService;
    @Getter
    private final List<Class<?>> generatedClasses = new ArrayList<>();
    private final Map<String, byte[]> classBytesMap = new ConcurrentHashMap<>();

    /**
     * 生成动态类
     */
    public List<Class<?>> generateClass() throws Exception {

        // 从数据库加载模版类
        List<DynamicClass> dynamicClassList = dynamicClassService.list();

        // 从数据库加载模版字段
        List<DynamicField> dynamicFieldList = dynamicFieldService.list();
        //dynamicFieldList 按classId进行分组

        Map<Long, List<DynamicField>> dynamicFieldMap = dynamicFieldList.stream().collect(Collectors.groupingBy(DynamicField::getClassId));

        for (DynamicClass dynamicClass : dynamicClassList) {
            // 使用Javassist生成类
            ClassPool pool = ClassPool.getDefault();
            // 构建完整类名（包名+类名）
            String fullClassName = dynamicClass.getPackageName() + "." + dynamicClass.getClassName();
            CtClass ctClass = pool.makeClass(fullClassName);

            // 添加Serializable接口支持
            ctClass.addInterface(pool.get(Serializable.class.getName()));

            // 添加serialVersionUID
            CtField serialVersionUID = new CtField(
                    pool.get("long"), "serialVersionUID", ctClass);
            serialVersionUID.setModifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL);
            ctClass.addField(serialVersionUID, "1L");

            // 添加字段
            for (DynamicField field : dynamicFieldMap.get(dynamicClass.getId())) {
                CtField ctField = new CtField(
                        getCtClassForType(pool, field.getFieldType()),
                        field.getFieldName(),
                        ctClass
                );

                // 添加getter/setter
                ctClass.addMethod(CtNewMethod.getter("get" + capitalize(field.getFieldName()), ctField));
                ctClass.addMethod(CtNewMethod.setter("set" + capitalize(field.getFieldName()), ctField));

                ctClass.addField(ctField);
            }

            // 生成类
            Class<?> generatedClass = ctClass.toClass();
            //ctClass.writeFile();

            byte[] bytecode = ctClass.toBytecode();
            classBytesMap.put(fullClassName, bytecode);

            generatedClasses.add(generatedClass);
        }

        return generatedClasses;
    }

    public Map<String, byte[]> getAllClassBytes() {
        return new HashMap<>(classBytesMap);
    }


    /**
     * 创建动态类实例
     */
//    public Object createInstance(String className) throws Exception {
//        Class<?> clazz = generateClass(className);
//        return clazz.getDeclaredConstructor().newInstance();
//    }

    private CtClass getCtClassForType(ClassPool pool, String type) throws NotFoundException {
        switch (type.toLowerCase()) {
            case "string": return pool.get(String.class.getName());
            case "int": case "integer": return pool.get(int.class.getName());
            case "long": return pool.get(long.class.getName());
            case "double": return pool.get(double.class.getName());
            case "boolean": return pool.get(boolean.class.getName());
            case "date": return pool.get(Date.class.getName());
            // 支持自定义类型
            default: return pool.get(type);
        }
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
