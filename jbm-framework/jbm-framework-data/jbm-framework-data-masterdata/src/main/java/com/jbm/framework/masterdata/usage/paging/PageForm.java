package com.jbm.framework.masterdata.usage.paging;

import com.jbm.framework.metadata.usage.bean.IBaseForm;

/**
 * <pre>
 * 对数据进行封装的基础类
 * 特别注意PageForm开始页是1
 * </pre>
 * 
 * @author wesley
 *
 */
public class PageForm implements IBaseForm {

	private static final long serialVersionUID = 1L;

	public final static PageForm NO_PAGING() {
		return new PageForm(1, Integer.MAX_VALUE);
	}

	/**
	 * 当前页
	 */
	private Integer currPage;
	/**
	 * 单页数量
	 */
	private Integer pageSize;
	/**
	 * 排序字段组合 id:asc,sort:desc
	 */
	private String sortRule;

	public PageForm() {
		this(1, Integer.MAX_VALUE);
	}

	public PageForm(Integer currPage, Integer pageSize) {
		super();
		this.currPage = currPage;
		this.pageSize = pageSize;
	}

	public PageForm(Integer currPage, Integer pageSize, String sortRule) {
		super();
		this.currPage = currPage;
		this.pageSize = pageSize;
		this.sortRule = sortRule;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public Integer getCurrPage() {
		return currPage;
	}

	public void setCurrPage(Integer currPage) {
		this.currPage = currPage;
	}

	public String getSortRule() {
		return sortRule;
	}

	public void setSortRule(String sortRule) {
		this.sortRule = sortRule;
	}

}
