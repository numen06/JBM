package com.jbm.framework.metadata.usage.page;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.google.common.collect.Lists;

/**
 * 
 * 封装分页数据的封装类
 * 
 * @author Wesley
 * 
 * @param <E>
 */
public class DataPaging<E> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 空的数据
	 */
	private final List<E> EMPTY_CONTENT = Lists.newArrayList();
	private final PageForm EMPTY_PAGEABLE = new PageForm();

	private final SerPage<E> pageWapper;
	private final PageForm pageForm;

	private final Map<String, Object> exp = new HashMap<String, Object>();

	public DataPaging() {
		this.pageWapper = new SerPage<E>(EMPTY_CONTENT);
		this.pageForm = EMPTY_PAGEABLE;
	}

	public DataPaging(Integer currPage, Integer limit) {
		this.pageForm = new PageForm(currPage, limit, 0);
		this.pageWapper = new SerPage<E>(EMPTY_CONTENT, this.pageForm, new Long(0));
	}

	public DataPaging(Iterable<? extends E> content, Integer total) {
		this.pageForm = EMPTY_PAGEABLE;
		this.pageWapper = new SerPage<E>(content, this.pageForm, new Long(total));
	}

	public DataPaging(Iterable<? extends E> content, Integer total, Integer offset, Integer limit) {
		this.pageForm = new PageForm(offset, limit);
		this.pageWapper = new SerPage<E>(content, this.pageForm, total);
	}

	public DataPaging(Iterable<? extends E> content, Integer total, Pageable pageable) {
		this.pageForm = new PageForm(pageable);
		this.pageWapper = new SerPage<E>(content, this.pageForm, total);
	}

	public DataPaging(Iterable<? extends E> content, Long total, Pageable pageable) {
		this.pageForm = new PageForm(pageable);
		this.pageWapper = new SerPage<E>(content, this.pageForm, total);
	}

	public DataPaging(Pageable pageable) {
		this.pageForm = new PageForm(pageable);
		this.pageWapper = new SerPage<E>(EMPTY_CONTENT, this.pageForm, 0);
	}

	public DataPaging(PageForm pageForm) {
		this.pageForm = pageForm;
		this.pageWapper = new SerPage<E>(EMPTY_CONTENT, this.pageForm, 0);
	}

	public DataPaging(Page<E> page) {
		this.pageForm = new PageForm(page.getNumber(), page.getSize());
		this.pageWapper = new SerPage<E>(page.getContent(), this.pageForm, page.getTotalElements());
	}

	public List<E> getList() {
		return this.pageWapper.page().getContent();
	}

	public Long getTotal() {
		return this.pageWapper.page().getTotalElements();
	}

	/**
	 * 总页数
	 */
	public Integer getTotalPage() {
		return pageWapper.page().getTotalPages();
	}

	/**
	 * 当前页数
	 */
	public Integer getCurrPage() {
		return pageForm.getPageNumber();
	}

	/**
	 * 开始条目
	 * 
	 * @return
	 */
	public Integer getStart() {
		return this.pageForm.getOffset();
	}

	/**
	 * 开始条目
	 * 
	 * @return
	 */
	public Integer getOffset() {
		return this.pageForm.getOffset();
	}

	/**
	 * 每页数量
	 * 
	 * @return
	 */
	public Integer getLimit() {
		return this.pageForm.getPageSize();
	}

	public Map<String, Object> getExp() {
		return exp;
	}

	public <K, V, T extends Map<K, V>> void putExp(Map<String, T> exp) {
		exp.putAll(exp);
	}

	public <K, V, T extends Map<K, V>> void putExp(String type, T mapper) {
		exp.put(type, mapper);
	}
}
