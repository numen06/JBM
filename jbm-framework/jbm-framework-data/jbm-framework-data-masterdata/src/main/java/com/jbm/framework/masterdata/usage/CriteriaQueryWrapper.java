package com.jbm.framework.masterdata.usage;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.enums.SqlKeyword;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.google.common.collect.Lists;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.util.StringUtils;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: zyf
 * @date: 2018/9/4 8:42
 * @@desc: 自定义查询构造器
 */
public class CriteriaQueryWrapper<T> extends QueryWrapper<T> {

    private static final long serialVersionUID = 1L;
    /**
     * 外键表别名对象
     */
    protected Map<String, String> aliasMap = new HashMap<>();
    /**
     * 查询字段
     */
    protected List<String> select = Lists.newArrayList();
    /**
     * 分页对象
     */
    private PageParams pageParams;


    /**
     * 通过分页创建空查询
     *
     * @param pageParams
     * @return
     */
    public static CriteriaQueryWrapper from(PageParams pageParams) {
        return new CriteriaQueryWrapper(pageParams);
    }


    public CriteriaQueryWrapper() {
        super();
    }

    public CriteriaQueryWrapper(T entity) {
        super(entity);
    }

    public CriteriaQueryWrapper(T entity, String... columns) {
        super(entity, columns);
    }


    public CriteriaQueryWrapper(PageParams pageParams) {
        this.setPageParams(pageParams);
    }


    public CriteriaQueryWrapper(T entity, PageParams pageParams) {
        super(entity);
        this.setPageParams(pageParams);
    }

    public void setPageParams(PageParams pageParams) {
        this.pageParams = pageParams;
        String sort = pageParams.getSort();
        apply("1=1");
        if (ObjectUtils.isNotEmpty(sort)) {
            //自动添加ordery by
            String order = pageParams.getOrder();
            Boolean isAsc = StringUtils.equalsIgnoreCase(SqlKeyword.ASC.name(), order);
            sort = StrUtil.toUnderlineCase(sort);
            orderBy(true, isAsc, sort);
        }
    }


    public Map<String, String> getAliasMap() {
        return aliasMap;
    }


    /**
     * 创建外键表关联对象,需要在mapper(xml)中编写join
     */
    public void createAlias(String entiry, String alias) {
        this.aliasMap.put(entiry, alias);
    }


    /**
     * 创建外键表关联对象,需要在mapper(xml)中编写join
     */
    public void createAlias(Class cla) {
        TableName tableAlias = AnnotationUtils.getAnnotation(cla, TableName.class);
        if (ObjectUtils.isNotEmpty(tableAlias)) {
            this.aliasMap.put(tableAlias.value(), tableAlias.value());
        }
    }

    /**
     * 等于
     */
    @Override
    public CriteriaQueryWrapper<T> eq(String column, Object val) {
        super.eq(ObjectUtils.isNotEmpty(val) && !val.equals(-1) && !val.equals(-1L), column, val);
        return this;
    }

    /**
     * like
     */
    @Override
    public CriteriaQueryWrapper<T> like(String column, Object val) {
        like(ObjectUtils.isNotEmpty(val), column, val);
        return this;
    }

    /**
     * in
     */
    @Override
    public CriteriaQueryWrapper<T> in(String column, Object... val) {
        in(ObjectUtils.isNotEmpty(val), column, val);
        return this;
    }


    /**
     * ge
     */
    @Override
    public CriteriaQueryWrapper<T> ge(String column, Object val) {
        ge(ObjectUtils.isNotEmpty(val), column, val);
        return this;
    }

    /**
     * le
     */
    @Override
    public CriteriaQueryWrapper<T> le(String column, Object val) {
        le(ObjectUtils.isNotEmpty(val), column, val);
        return this;
    }

    /**
     * lt
     */
    @Override
    public CriteriaQueryWrapper<T> lt(String column, Object val) {
        lt(ObjectUtils.isNotEmpty(val), column, val);
        return this;
    }

    /**
     * gt
     */
    @Override
    public CriteriaQueryWrapper<T> gt(String column, Object val) {
        gt(ObjectUtils.isNotEmpty(val), column, val);
        return this;
    }


    /**
     * or
     */
    @Override
    public CriteriaQueryWrapper<T> or() {
        super.or();
        return this;
    }

    /**
     * likeLeft
     */
    @Override
    public QueryWrapper<T> likeLeft(String column, Object val) {
        return likeLeft(ObjectUtils.isNotEmpty(val), column, val);
    }

    /**
     * likeRight
     */
    @Override
    public QueryWrapper<T> likeRight(String column, Object val) {
        return likeRight(ObjectUtils.isNotEmpty(val), column, val);
    }

    /**
     * 指定查询
     */
    public CriteriaQueryWrapper<T> select(String sql) {
        this.select.add(sql);
        return this;
    }

    public PageParams getPageParams() {
        return pageParams;
    }

    public String getSelect() {
        StringBuffer str = new StringBuffer();
        String sqlSelect = getSqlSelect();
        if (ObjectUtils.isNotEmpty(sqlSelect)) {
            select.add(String.join(",", sqlSelect));
        }
        if (CollectionUtils.isEmpty(select)) {
            select.add("*");
        }
        return String.join(",", select);
    }


}
