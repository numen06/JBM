package com.jbm.framework.masterdata.usage;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Maps;
import com.jbm.framework.usage.form.JsonRequestBody;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.MapUtils;
import com.jbm.util.StringUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * 分页参数
 *
 * @author liuyau
 * @date 2018/07/10
 */
public class PageParams extends Page implements Serializable {
    private static final long serialVersionUID = -1710273706052960025L;
    private int page = DEFAULT_PAGE;
    private int limit = DEFAULT_LIMIT;
    private String sort;
    private String order;
    private Map<String, Object> requestMap = Maps.newHashMap();

    public static final String PAGE_KEY = "page";
    /**
     * 显示条数 KEY
     */
    public static final String PAGE_LIMIT_KEY = "limit";
    /**
     * 排序字段 KEY
     */
    public static final String PAGE_SORT_KEY = "sort";
    /**
     * 排序方向 KEY
     */
    public static final String PAGE_ORDER_KEY = "order";

    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE = 1;
    /**
     * 默认显示条数
     */
    public static final int DEFAULT_LIMIT = Integer.MAX_VALUE;

    /**
     * 默认最小页码
     */
    public static final int MIN_PAGE = 0;
    /**
     * 最大显示条数
     */
    public static final int MAX_LIMIT = 999;
    /**
     * 排序
     */
    private String orderBy;

    public PageParams() {
        requestMap = Maps.newHashMap();
    }

    public PageParams(JsonRequestBody jsonRequestBody) {
        this(jsonRequestBody.getPageForm());
        requestMap.putAll(jsonRequestBody);
    }

    public PageParams(PageForm pageForm) {
        this.page = pageForm.getCurrPage();
        this.limit = pageForm.getPageSize();
        final Map<String, String> rule = MapUtils.split(pageForm.getSortRule(), Maps.newLinkedHashMap(), ",", ":");
        for (String col : rule.keySet()) {
            String sort = rule.get(col);
            final String unCol = StrUtil.toUnderlineCase(col);
            if ("DESC".equalsIgnoreCase(sort)) {
                this.addOrder(OrderItem.desc(unCol));
            } else {
                this.addOrder(OrderItem.asc(unCol));
            }
        }
        super.setCurrent(page);
        super.setSize(limit);
    }

    public PageParams(Map map) {
        if (map == null) {
            map = Maps.newHashMap();
        }
        this.page = Integer.parseInt(map.getOrDefault(PAGE_KEY, DEFAULT_PAGE).toString());
        this.limit = Integer.parseInt(map.getOrDefault(PAGE_LIMIT_KEY, DEFAULT_LIMIT).toString());
        this.sort = (String) map.getOrDefault(PAGE_SORT_KEY, "");
        this.order = (String) map.getOrDefault(PAGE_ORDER_KEY, "");
        super.setCurrent(page);
        super.setSize(limit);
        map.remove(PAGE_KEY);
        map.remove(PAGE_LIMIT_KEY);
        map.remove(PAGE_SORT_KEY);
        map.remove(PAGE_ORDER_KEY);
        requestMap.putAll(map);
    }

    public PageParams(int page, int limit) {
        this(page, limit, "", "");
    }

    public PageParams(int page, int limit, String sort, String order) {
        this.page = page;
        this.limit = limit;
        this.sort = sort;
        this.order = order;
    }

    public int getPage() {
        if (page <= MIN_PAGE) {
            page = 1;
        }
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getLimit() {
        if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getOrderBy() {
        if (StringUtils.isBlank(order)) {
            order = "asc";
        }
        if (StringUtils.isNotBlank(sort)) {
            this.setOrderBy(String.format("%s %s", StrUtil.toUnderlineCase(sort), order));
        }
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public <T> T mapToObject(Class<T> t) {
        return BeanUtil.mapToBean(this.requestMap, t,true);
    }

    public Map<String, Object> getRequestMap() {
        return requestMap;
    }

    public void setRequestMap(Map<String, Object> requestMap) {
        this.requestMap = requestMap;
    }
}
