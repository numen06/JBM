package com.td.metadata.entity.common;

/**
 * 状态枚举
 * 
 * @author wesley
 *
 */
public enum MetaStateEnum {

	Active(1, "active"), Destroy(0, "destroy"), Pigeonhole(2, "pigeonhole");

	private MetaState metaState;

	private MetaStateEnum(int value, String name) {
		this.metaState = new MetaState(value, name);
	}

	private MetaStateEnum(MetaState metaState) {
		this.metaState = metaState;
	}

	public MetaState getMetaState() {
		return metaState;
	}

	public int state() {
		return metaState.getCode();
	}

	@Override
	public String toString() {
		return metaState.getName();
	}

}
