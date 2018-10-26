package jbm.framework.boot.autoconfigure.druid;

import java.util.Properties;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

import com.alibaba.druid.pool.DruidDataSource;

@ConfigurationProperties(prefix = DruidProperties.PREFIX)
public class DruidProperties extends Properties implements InitializingBean {

	/**
	 * 
	 */
	public static final long serialVersionUID = -4231415209606040910L;
	public static final String PREFIX = "druid";

	private final Environment environment;
	private final DataSourceProperties dataSourceProperties;
	private final DruidDataSource druidDataSource;

	public DruidProperties(Environment environment, DataSourceProperties dataSourceProperties) {
		super();
		this.environment = environment;
		this.dataSourceProperties = dataSourceProperties;
		this.druidDataSource = new DruidDataSource();
	}

	public DruidProperties(Environment environment, DataSourceProperties dataSourceProperties, DruidDataSource druidDataSource) {
		super();
		this.environment = environment;
		this.dataSourceProperties = dataSourceProperties;
		this.druidDataSource = druidDataSource;
	}

	@Override
	public void afterPropertiesSet() {
		if (dataSourceProperties != null) {
			druidDataSource.setUrl(dataSourceProperties.getUrl());
			druidDataSource.setUsername(dataSourceProperties.getUsername());
			druidDataSource.setPassword(dataSourceProperties.getPassword());
			druidDataSource.setDriverClassName(dataSourceProperties.getDriverClassName());
		}
	}

	@Override
	public String getProperty(String key) {
		return environment.getProperty(key);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		return environment.getProperty(key, defaultValue);
	}

	@Override
	public synchronized int size() {
		return 1;
	}

}
