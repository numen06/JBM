package com.td.util.list;

import java.util.ArrayList;
import java.util.List;

public class ParameterList<E> extends ArrayList<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ParameterList<E> append(E value) {
		this.add(value);
		return this;
	}

	public ParameterList<E> append(List<E> value) {
		this.addAll(value);
		return this;
	}

}
