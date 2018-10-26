package com.jbm.util.comparator;

import java.util.Comparator;

public abstract class AbstractComparator<T> implements Comparator<T> {
	// 降序
	public final static String DESC = "desc";
	// 升序
	public final static String ASC = "asc";

	private String sort;

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

	public AbstractComparator() {
		super();
	}

	public AbstractComparator(String sort) {
		super();
		this.sort = sort;
	}


}
