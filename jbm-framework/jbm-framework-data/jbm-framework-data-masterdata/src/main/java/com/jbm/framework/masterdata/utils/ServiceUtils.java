package com.jbm.framework.masterdata.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.google.common.collect.Maps;
import com.jbm.framework.exceptions.DataServiceException;
import com.jbm.framework.masterdata.usage.CriteriaQueryWrapper;
import com.jbm.framework.masterdata.usage.PageParams;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.ArrayUtils;
import com.jbm.util.MapUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2020-01-15 01:40
 **/
@Slf4j
public class ServiceUtils {


    /**
     * 通过指定主键生成MAP
     *
     * @param entitys
     * @param key
     * @param <Entity>
     * @param toList   是否转换为List
     * @return
     */
    public static <Entity> Map<String, Entity> entityToDictionary(List<Entity> entitys, String key, boolean toList) {
        Map<String, Entity> result = Maps.newLinkedHashMap();
        for (Entity entity : entitys) {
            result.put(BeanUtil.getProperty(entity, key), entity);
        }
        return result;
    }

    /**
     * 将实体转换成Map
     *
     * @param entitys
     * @param key
     * @param value
     * @param <Entity>
     * @param toList   是否转换为List
     * @return
     */
    public static <Entity> Map<String, Entity> entityToDictionary(List<Entity> entitys, String key, String value, boolean toList) {
        Map<String, Entity> result = Maps.newLinkedHashMap();
        for (Entity entity : entitys) {
            result.put(BeanUtil.getProperty(entity, key), BeanUtil.getProperty(entity, value));
        }
        return result;
    }

    /**
     * 创建默认的分页查询
     *
     * @param entity
     * @param pageForm
     * @param <Entity>
     * @return
     */
    public static <Entity> CriteriaQueryWrapper toCriteriaQueryWrapper(Entity entity, PageForm pageForm) {
        CriteriaQueryWrapper criteriaQueryWrapper = new CriteriaQueryWrapper(entity, PageParams.from(pageForm));
        return criteriaQueryWrapper;
    }


    /**
     * 通过map创建一个默认查询
     *
     * @param params
     * @param pageForm
     * @param <Entity>
     * @return
     */
    public static <Entity> CriteriaQueryWrapper toCriteriaQueryWrapper(Map<String, Object> params, PageForm pageForm) {
        CriteriaQueryWrapper criteriaQueryWrapper = new CriteriaQueryWrapper(PageParams.from(pageForm));
        criteriaQueryWrapper.allEq(params);
        return criteriaQueryWrapper;
    }


    /**
     * page转换为数据翻页
     *
     * @param page
     * @param pageForm
     * @param <T>
     * @return
     */
    public static <T> DataPaging<T> pageToDataPaging(IPage page, PageForm pageForm) {
        final DataPaging<T> dataPaging = new DataPaging<>(page.getRecords(), page.getTotal(), page.getPages(), pageForm);
        return dataPaging;
    }


    /**
     * @param page
     * @param <T>
     * @return
     */
    public static <T> DataPaging<T> pageToDataPaging(IPage page) {
        final PageForm pageForm = pageToPageForm(page);
        final DataPaging<T> dataPaging = new DataPaging<>(page.getRecords(), page.getTotal(), page.getPages(), pageForm);
        return dataPaging;
    }


    public static PageForm pageToPageForm(IPage page) {
        final PageForm pageForm = new PageForm(new Long(page.getCurrent()).intValue(), new Long(page.getSize()).intValue());
        return pageForm;
    }

    public static <T> Page<T> buildPage(PageForm pageForm) {
        if (pageForm == null)
            pageForm = PageForm.NO_PAGING();
        final com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<T>(pageForm.getCurrPage(), pageForm.getPageSize());
        final Map<String, String> rule = MapUtils.split(pageForm.getSortRule(), Maps.newLinkedHashMap(), ",", ":");
        for (String col : rule.keySet()) {
            String sort = rule.get(col);
            final String unCol = StrUtil.toUnderlineCase(col);
            if ("DESC".equalsIgnoreCase(sort)) {
                page.addOrder(OrderItem.desc(unCol));
            } else {
                page.addOrder(OrderItem.asc(unCol));
            }
        }
        return page;
    }

    /**
     * 拼接mapper全路径
     *
     * @param sqlMethod
     * @return
     */
    public static String sqlStatement(Class entityClass, String sqlMethod) {
        if (com.jbm.util.StringUtils.contains(sqlMethod, ".")) {
            return sqlMethod;
        } else {
            return SqlHelper.table(entityClass).getSqlStatement(sqlMethod);
        }
    }


    // ---------------------------------------------------------build转换方法----------------------------------------------------------------------

    public static <Entity> QueryWrapper<Entity> buildMapperQueryWrapper(Map<String, Object> params) {
//        JSONObject json = new JSONObject(params);
//        Entity entity = json.toJavaObject(getEntityClass());
        QueryWrapper<Entity> queryWrapper = new QueryWrapper<Entity>();
        for (String key : params.keySet()) {
            Object val = params.get(key);
            if (val instanceof Collection) {
                queryWrapper.in(key, val);
            } else if (val.getClass().isArray()) {
                queryWrapper.in(key, val);
            } else {
                queryWrapper.eq(key, val);
            }
        }
        log.info("params:{}", JSON.toJSONString(params));
        return queryWrapper;
    }


    public static Object buildMapperParameter(final Object[] args) {
        final Map<String, Object> map = Maps.newLinkedHashMap();
        // final RowBounds rowBounds;
        Object parameter = map;
        if (ArrayUtils.getLength(args) == 1)
            parameter = args[0];
        else {
            try {
                parameter = MapUtils.fromArray(args);
            } catch (Exception e) {
                log.error("组装参数错误", e);
                throw new DataServiceException("组装参数错误", e);
            }
        }
        return parameter;
    }


}
