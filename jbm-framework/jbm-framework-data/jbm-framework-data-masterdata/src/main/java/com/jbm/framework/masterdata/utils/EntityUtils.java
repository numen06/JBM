package com.jbm.framework.masterdata.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.core.toolkit.support.SerializedLambda;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 针对实体进行操作
 *
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-02-21 01:32
 **/
@Slf4j
public class EntityUtils {


    /**
     * 主键字段
     */
    public static String getEntityProperty(Object obj) {
        return getKeyProperty(obj.getClass());
    }


    /**
     * 主键字段
     */
    public static String getKeyProperty(Class clazz) {
        return TableInfoHelper.getTableInfo(clazz).getKeyProperty();
    }

    /**
     * 主键值
     */
    public static <T> T getKeyValue(Object entity) {
        return (T) ReflectionKit.getMethodValue(entity, getKeyProperty(entity.getClass()));
    }

    /**
     * 主键值
     */
    public static boolean keyIsEmpty(Object entity) {
        return ObjectUtil.isEmpty(EntityUtils.getKeyValue(entity));
    }

    /**
     * 通过实体生成一个默认的查询
     *
     * @param entity
     * @param <Entity>
     * @return
     */
    public static <Entity> QueryWrapper<Entity> buildEntityQueryWrapper(Entity entity) {
        QueryWrapper<Entity> queryWrapper = new QueryWrapper<Entity>();
        if (entity == null) {
            return queryWrapper;
        }
        Object id = EntityUtils.getKeyValue(entity);
        if (ObjectUtil.isNotEmpty(id)) {
            queryWrapper.eq(StrUtil.toUnderlineCase(getEntityProperty(entity)), id);
            return queryWrapper;
        }
        queryWrapper = new QueryWrapper<Entity>(entity);
        return queryWrapper;
    }

    /**
     * 将列表转换成字典
     *
     * @param list
     * @param <Entity>
     * @return
     */
    public static <Entity> Map<Long, Entity> entityToDictionary(List<Entity> list) {
        Map<Long, Entity> result = Maps.newHashMap();
        for (Iterator<Entity> iterator = list.iterator(); iterator.hasNext(); ) {
            Entity e = iterator.next();
            result.put(EntityUtils.getKeyValue(e), e);
        }
        return result;
    }

//    public static <T> String setDefault(T bean, boolean criteria, SFunction<T, ?> func) {
//        if (criteria) {
//            BeanUtil.getProperty(bean, toFieldName(func));
//        }
//
//    }

    /***
     * 转换方法引用为属性名
     * @param func
     * @return
     */
    public static <T> String toFieldName(SFunction<T, ?> func) {
        SerializedLambda lambda = LambdaUtils.resolve(func);
        // 获取方法名
        String methodName = lambda.getImplMethodName();
        String prefix = null;
        if (methodName.startsWith("get")) {
            prefix = "get";
        } else if (methodName.startsWith("set")) {
            prefix = "set";
        } else if (methodName.startsWith("is")) {
            prefix = "is";
        }
        if (prefix == null) {
            log.info("无效的getter方法: {}", methodName);
        }
        String field = StrUtil.subAfter(methodName, prefix, false);
        return StrUtil.lowerFirst(field);
    }

    /**
     * 转换为字段的名称
     *
     * @param func
     * @param <T>
     * @return
     */
    public static <T> String toDbName(SFunction<T, ?> func) {
        return StrUtil.toUnderlineCase(toFieldName(func));
    }


}
