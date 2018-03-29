package com.td.framework.metadata.bean;

/**
 * Created with IntelliJ IDEA. User: Joe Xie Date: 14-9-3 Time: 下午6:34
 */
public class ClsInfo {
	private String tableName;

	private String idColumn;

	private String idProperty;

	/**
	 * 是否需要动态生成主键
	 */
	private Boolean generate = true;

	public ClsInfo() {
		super();
	}

	public ClsInfo(String tableName, String idColumn, String idProperty) {
		super();
		this.tableName = tableName;
		this.idColumn = idColumn;
		this.idProperty = idProperty;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getIdColumn() {
		return idColumn;
	}

	public void setIdColumn(String idColumn) {
		this.idColumn = idColumn;
	}

	public String getIdProperty() {
		return idProperty;
	}

	public void setIdProperty(String idProperty) {
		this.idProperty = idProperty;
	}

	public Boolean getGenerate() {
		return generate;
	}

	public void setGenerate(Boolean generate) {
		this.generate = generate;
	}

}