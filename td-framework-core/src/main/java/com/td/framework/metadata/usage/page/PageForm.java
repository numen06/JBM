package com.td.framework.metadata.usage.page;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

import com.google.common.collect.Lists;
import com.td.framework.metadata.usage.bean.IBaseForm;
import com.td.util.MapUtils;
import com.td.util.ObjectUtils;
import com.td.util.StringUtils;

/**
 * <pre>
 * 对数据进行封装的基础类
 * 特别注意PageForm开始页是1
 * Pageable开始页是0
 * </pre>
 * 
 * @author wesley
 *
 */
public class PageForm implements IBaseForm {

	private static final long serialVersionUID = 1L;

	private SerPageable pageWapper;

	public final static PageForm NO_PAGING() {
		return new PageForm();
	}

	private Integer offset;
	private Integer limit;
	private Integer currPage;
	private String sortRule;

	public PageForm() {
		super();
		this.pageWapper = new SerPageable(0, Integer.MAX_VALUE);
	}

	public PageForm(Integer offset, Integer limit) {
		super();
		this.pageWapper = new SerPageable(SerPageable.buildPage(offset, limit), limit);
	}

	public PageForm(Integer offset, Integer limit, Sort sort) {
		super();
		this.pageWapper = new SerPageable(SerPageable.buildPage(offset, limit), limit, sort);
	}

	public PageForm(Integer currPage, Integer pageSize, Integer offset) {
		super();
		this.pageWapper = new SerPageable(currPage, pageSize);
	}

	public PageForm(Pageable pageable) {
		this.pageWapper = new SerPageable(pageable.getPageNumber() + 1, pageable.getPageSize(), pageable.getSort());
	}

	public Pageable pageable() {
		return this.pageWapper.pageable();
	}

	public void setStart(Integer start) {
		this.offset = start;
		createPage();
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
		createPage();
	}

	public void setOffset(Integer offset) {
		this.offset = offset;
		createPage();
	}

	public void setCurrPage(Integer currPage) {
		this.currPage = currPage;
		createPage();
	}

	public void setPageSize(Integer pageSize) {
		this.limit = pageSize;
		createPage();
	}

	public Integer getCurrPage() {
		return this.pageWapper.pageable().getPageNumber() + 1;
	}

	public int getPageSize() {
		return this.pageWapper.pageable().getPageSize();
	}

	public Integer getLimit() {
		return this.pageWapper.pageable().getPageSize();
	}

	public int getOffset() {
		return this.pageWapper.pageable().getOffset();
	}

	public Integer getStart() {
		return this.pageWapper.pageable().getOffset();
	}

	// 后续的都是pageable高级方法

	public int getPageNumber() {
		return this.pageWapper.pageable().getPageNumber() + 1;
	}

	public PageForm next() {
		return new PageForm(this.pageWapper.pageable().next());
	}

	public PageForm previousOrFirst() {
		return new PageForm(this.pageWapper.pageable().previousOrFirst());
	}

	public PageForm first() {
		return new PageForm(this.pageWapper.pageable().first());
	}

	public boolean hasPrevious() {
		return this.pageWapper.pageable().hasPrevious();
	}

	public String getSortRule() {
		return sortRule;
	}

	public void setSortRule(String sortRule) {
		this.sortRule = sortRule;
		this.pageWapper.setSortRule(sortRule);
	}

	private void createPage() {
		if (ObjectUtils.allIsNotNull(this.currPage, this.limit)) {
			this.pageWapper = new SerPageable(currPage, limit);
			cleanValue();
		} else if (ObjectUtils.allIsNotNull(this.offset, this.limit)) {
			this.pageWapper = new SerPageable(SerPageable.buildPage(offset, limit), limit);
			cleanValue();
		}
		if (ObjectUtils.allIsNotNull(this.sortRule)) {
			this.pageWapper.setSortRule(this.sortRule);
			cleanValue();
		}
	}

	/**
	 * 清空所有属性
	 */
	private void cleanValue() {
		// this.currPage = null;
		// this.limit = null;
		// this.offset = null;
	}

	public static Sort sortFormString(String sortRule) {
		if (StringUtils.isBlank(sortRule))
			return null;
		Map<String, String> rules = MapUtils.split(sortRule, new HashMap<String, String>(), ",", ":");
		List<Order> orders;
		try {
			orders = Lists.newArrayList();
			for (String key : rules.keySet()) {
				Order temp = new Order(Direction.fromStringOrNull(StringUtils.trimToEmpty(rules.get(key))), StringUtils.trimToEmpty(key));
				orders.add(temp);
			}
			return new Sort(orders);
		} catch (Exception e) {
			return null;
		}
	}

}
