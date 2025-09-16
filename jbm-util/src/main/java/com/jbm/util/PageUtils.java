package com.jbm.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.PageUtil;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;

import java.util.List;
import java.util.function.Function;

/**
 * 分页工具类
 * @author wesley
 */
public class PageUtils {



    public static <T> PageResult<T> pageResult(Page page,List<T> list, Integer totalCount) {
        final PageResult<T> result = new PageResult<>(page.getPageNumber(), page.getPageSize());
        if (CollUtil.isNotEmpty(list)) {
            result.addAll(list);
        }
        if (totalCount == null) {
            result.setTotal(result.size());
        } else {
            result.setTotal(totalCount);
        }
        result.setTotalPage(PageUtil.totalPage(result.getTotal(), page.getPageSize()));
        return result;
    }


    /**
     * 循环分页查询直到最后一页
     * @param all
     * @param doPage
     * @param start
     * @param pageSize
     * @param <T>
     */
    public static <T> void loopPage(List<T> all, Function<Page,PageResult<T>> doPage, Integer start, Integer pageSize) {
        Page startPage = Page.of(start, pageSize);
        loopPage(all, doPage, startPage);
    }

    public static <T> void loopPage(List<T> all, Function<Page,PageResult<T>> doPage, Page startPage) {
        long total = 0;
        do {
            PageResult<T> currentPageData = new PageResult<>(startPage.getPageNumber(), startPage.getPageSize());
            try {
                currentPageData = doPage.apply(startPage);
            } catch (Exception e) {
                Console.error("分页查询异常", e);
                break;
            }
            // 第一次获取时记录总数（避免每页都查总数，提高性能）
            if (total == 0) {
                total = currentPageData.getTotal();
            }
            int totalPage = PageUtil.totalPage(total, startPage.getPageSize());
            currentPageData.setTotalPage(totalPage);
            currentPageData.setPage(startPage.getPageNumber());
            all.addAll(currentPageData);
            // 判断页码是不是最后一页
            if (currentPageData.isLast()) {
                break;
            }
            // 翻页
            Console.log("总页数:{},当前页:{}", totalPage, startPage.getPageNumber());
            startPage.setPageNumber(startPage.getPageNumber() + 1);
        } while (true);
    }
}
