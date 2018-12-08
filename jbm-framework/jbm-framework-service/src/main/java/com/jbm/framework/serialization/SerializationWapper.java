package com.jbm.framework.serialization;

import java.io.Serializable;

public class SerializationWapper implements Serializable {
	private static final long serialVersionUID = 1L;
	private Object x;

	public SerializationWapper(Object x) {
		super();
		this.x = x;
	}

	public Object x() {
		return x;
	}

}