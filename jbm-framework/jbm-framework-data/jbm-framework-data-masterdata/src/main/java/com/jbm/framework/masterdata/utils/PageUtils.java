package com.jbm.framework.masterdata.utils;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Maps;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.usage.form.JsonRequestBody;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.MapUtils;

import java.util.Map;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2020-01-15 01:40
 **/
public class PageUtils {

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
}
