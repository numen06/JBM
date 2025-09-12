package com.jbm.util;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;

import java.util.List;
import java.util.function.Supplier;

/**
 * 分页工具类
 * @author wesley
 */
public class PageUtils {

    public static <T> void doAllPage(List<T> all, Supplier<PageResult<T>> doPage, Page startPage) {
        doAllPage(all, doPage, startPage, true);
    }

    public static <T> void doAllPage(List<T> all, Supplier<PageResult<T>> doPage, Page startPage, boolean onePage) {
        long total = 0;
        do {
            PageResult<T> currentPageData = doPage.get();
            // 第一次获取时记录总数（避免每页都查总数，提高性能）
            if (total == 0) {
                total = currentPageData.getTotal();
            }
            int totalPage = PageUtil.totalPage(total, startPage.getPageSize()) + BooleanUtil.toInt(onePage);
            currentPageData.setTotalPage(totalPage);
            all.addAll(currentPageData);
            // 判断页码是不是最后一页
            if (currentPageData.isLast()) {
                break;
            }
            // 翻页
            startPage.setPageNumber(startPage.getPageNumber() + 1);

        } while (true);
    }
}
