package com.jbm.framework.metadata.usage.page;

import java.io.Serializable;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 包装分页信息
 * 
 * @author wesley
 *
 */
public class SerPageable implements Serializable {

	private static final long serialVersionUID = 1L;
	private int page = 1;
	private int size = Integer.MAX_VALUE;
	private String sortRule;
	private transient Pageable pageable;

	public SerPageable(Integer page, Integer size) {
		this(page, size, "");
	}

	public SerPageable(Integer page, Integer size, String sortRule) {
		super();
		this.page = checkPage(page);
		this.size = checkPageSize(size);
		this.sortRule = sortRule;
	}

	public SerPageable(Integer page, Integer size, Sort sort) {
		super();
		this.page = checkPage(page);
		this.size = checkPageSize(size);
		this.sortRule = checkSort(sort);
	}

	public SerPageable(String sortRule) {
		super();
		this.page = checkPage(page);
		this.size = checkPageSize(size);
	}

	public void setSortRule(String sortRule) {
		this.sortRule = sortRule;
		pageable = null;
	}

	public void setPage(int page) {
		this.page = page;
		pageable = null;
	}

	public void setSize(int size) {
		this.size = size;
		pageable = null;
	}

	public static int buildPage(Integer offset, Integer limit) {
		int page = 1;
		try {
			page = offset / limit;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return page + 1;
	}

	private static String checkSort(Sort sort) {
		if (sort == null) {
			return null;
		}
		return sort.toString();
	}

	private static int checkPage(Integer page) {
		if (page == null || page < 0) {
			return 0;
		}
		return page;
	}

	private static int checkPageSize(Integer size) {
		if (size == null || size < 1) {
			return Integer.MAX_VALUE;
		}
		return size;
	}

	public Pageable pageable() {
		if (pageable == null)
			pageable = new PageRequest(checkPage(page - 1), size, PageForm.sortFormString(sortRule));
		return pageable;
	}

}
