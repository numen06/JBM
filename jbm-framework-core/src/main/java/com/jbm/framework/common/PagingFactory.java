package com.jbm.framework.common;

import com.jbm.framework.metadata.usage.page.DataPaging;

public class PagingFactory {

	// private static final Logger logger =
	// Logger.getLogger(PagingFactory.class);

	public static <E> DataPaging<E> creatDataPaging(Integer currPage, Integer limit) {
		return new DataPaging<E>(currPage, limit);
	}

	/**
	 * 计算起始点
	 * 
	 * @param currPage
	 * @param limit
	 * @return
	 */
	public static Integer buildOffset(Integer currPage, Integer limit) {
		if (currPage == null || limit == null)
			return 0;
		int offset = 0;
		offset = (currPage - 1) * limit;
		return offset;
	}

	/**
	 * 计算总页数
	 * 
	 * @param total
	 *            总数
	 * @param limit
	 *            每页树
	 * @return
	 */
	public static Integer buildTotalPage(Integer total, Integer limit) {
		if (total == null || limit == null || limit == 0)
			return 1;
		int totalPage = 0;

		if (total % limit > 0) {
			totalPage = (total / limit) + 1;
		} else {
			totalPage = (total / limit);
		}
		return totalPage;
	}

	public static Integer buildTotalPage(Long total, Integer limit) {
		return buildTotalPage(total.intValue(), limit);
	}

}
