package com.td.framework.datasource;

import java.util.List;

import com.td.framework.metadata.bean.DataSource;

public interface DataSourceService<Entity extends DataSource> {

	public List<Entity> getDataSources();

	public Entity getDefaultDataSource();
}
