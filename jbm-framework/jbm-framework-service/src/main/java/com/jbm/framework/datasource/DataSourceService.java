package com.jbm.framework.datasource;

import java.util.List;

import com.jbm.framework.metadata.bean.DataSource;

public interface DataSourceService<Entity extends DataSource> {

	public List<Entity> getDataSources();

	public Entity getDefaultDataSource();
}
