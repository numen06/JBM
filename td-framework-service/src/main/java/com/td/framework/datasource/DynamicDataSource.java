package com.td.framework.datasource;

import com.td.framework.metadata.bean.DataSource;
import com.td.util.BeanUtils;
import com.td.util.CollectionUtils;
import com.td.util.MapUtils;
import com.td.util.ObjectUtils;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Maps;
import com.td.framework.service.support.ISessionSupport;

public class DynamicDataSource extends AbstractRoutingDataSource {

	public final static String DS_KEY = "ds_";
	public final static String DEFAULT_SESSION = "";

	private DataSourceService<DataSource> dataSourceService;

	public void setDataSourceService(DataSourceService<DataSource> dataSourceService) {
		this.dataSourceService = dataSourceService;
	}

	private Object demoTargetDataSource;

	private List<DataSource> dataSources = new ArrayList<DataSource>();

	public void setDefaultTargetDataSource(Object defaultTargetDataSource) {
		this.demoTargetDataSource = defaultTargetDataSource;
		super.setDefaultTargetDataSource(defaultTargetDataSource);
	}

	@Autowired(required = false)
	private ISessionSupport sessionSupport;

	private final static Map<String, String> contextHolder = Maps.newConcurrentMap();

	private Map<String, String> dataSourceNames;

	public final synchronized void setDataSourceKey(String dataSourceKey) {
		if (sessionSupport == null)
			contextHolder.put(DEFAULT_SESSION, dataSourceKey);
		else
			contextHolder.put(sessionSupport.getSessionId(), dataSourceKey);
	}

	public final String getDataSourceKey() {
		if (sessionSupport == null)
			return MapUtils.getString(contextHolder, DEFAULT_SESSION);
		return MapUtils.getString(contextHolder, sessionSupport.getSessionId());
	}

	public Map<String, String> getDataSourceNames() {
		return dataSourceNames;
	}

	public void setDataSourceNames(Map<String, String> dataSourceNames) {
		this.dataSourceNames = dataSourceNames;
	}

	public final void setDefaultTargetDataSourceKey(String defaultTargetDataSourceKey) {
		setDataSourceKey(defaultTargetDataSourceKey);
	}

	@Override
	public void afterPropertiesSet() {
		if (ObjectUtils.isNotNull(dataSourceService)) {
			List<DataSource> dss = dataSourceService.getDataSources();
			dataSources = dss;
		}
		if (CollectionUtils.isEmpty(dataSources)) {
			super.afterPropertiesSet();
			return;
		}
		Map<Object, Object> targetDataSources = MapUtils.newHashMap();
		for (DataSource ds : dataSources) {
			Object obj;
			try {
				obj = demoTargetDataSource.getClass().newInstance();
				System.out.println(obj);
				BeanUtils.setProperty(obj, "driverClass", ds.getDriverClass());
				BeanUtils.setProperty(obj, "jdbcUrl", ds.getJdbcUrl());
				BeanUtils.setProperty(obj, "user", ds.getUser());
				BeanUtils.setProperty(obj, "password", ds.getPassword());
				String key = DS_KEY + ds.getId();
				ds.setKey(key);
				targetDataSources.put(key, obj);
				if (ds.getIsDefault()) {
					this.setDefaultTargetDataSourceKey(key);
				}
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		super.setTargetDataSources(targetDataSources);
		super.afterPropertiesSet();
	}

	public void refresh() {
		this.afterPropertiesSet();
	}

	@Override
	public Object determineCurrentLookupKey() {
		return getDataSourceKey();
	}

	public List<DataSource> getDataSources() {
		return dataSources;
	}

	public void setDataSources(List<DataSource> dataSources) {
		this.dataSources = dataSources;
	}

}