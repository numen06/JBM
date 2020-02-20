package com.jbm.framework.masterdata.utils;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.google.common.collect.Maps;

import java.io.Serializable;
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
            queryWrapper.eq(getEntityProperty(entity), id);
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
}
