package com.jbm.framework.tools;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.core.io.Resource;

import com.jbm.util.BeanUtils;

/**
 * 
 * 配置文件封装虚拟类
 * 
 * @author Wesley
 * 
 */
public abstract class AbstactConfigurationProperties implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Resource configLocation;

	private Properties configurationProperties;

	public Resource getConfigLocation() {
		return configLocation;
	}

	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
		this.initProperties(configLocation);
	}

	public Properties getConfigurationProperties() {
		return configurationProperties;
	}

	public void setConfigurationProperties(Properties configurationProperties) {
		this.configurationProperties = configurationProperties;
	}

	/**
	 * 当配置被注入的时候通知方法
	 * 
	 * @throws IOException
	 */
	protected void initProperties(Resource configLocation) {
		this.configurationProperties = new Properties();
		try {
			this.configurationProperties.load(configLocation.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		initPropertys();
	}

	public abstract void initPropertys();

	/**
	 * 通过配置文件填充所有配置的属性
	 * 
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	public void fillPropertys(Properties configurationProperties) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Enumeration<?> propertyNames = configurationProperties.propertyNames();

		while (propertyNames.hasMoreElements()) {
			String key = (String) propertyNames.nextElement();

			this.setProperty(configurationProperties, key);
		}
	}

	/**
	 * @param propertyName
	 *            需要填充的属性名称
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	protected void setProperty(Properties configurationProperties, String propertyName) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		if (PropertyUtils.isWriteable(this, propertyName))
			BeanUtils.setProperty(this, propertyName, configurationProperties.getProperty(propertyName));
	}

	/**
	 * 
	 * @param propertyName
	 *            需要填充的属性名称
	 * @param defaultValue
	 *            默认值
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	protected void setProperty(Properties configurationProperties, String propertyName, String defaultValue) throws IllegalAccessException, InvocationTargetException,
		NoSuchMethodException {
		if (PropertyUtils.isWriteable(this, propertyName))
			BeanUtils.setProperty(this, propertyName, configurationProperties.getProperty(propertyName, defaultValue));
	}
}
