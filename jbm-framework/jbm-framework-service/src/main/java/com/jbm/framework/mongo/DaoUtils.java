package com.jbm.framework.mongo;

import com.jbm.util.ClassUtils;

/**
 * 
 * Dao层工具类
 * 
 * @author Wesley
 * 
 */
public class DaoUtils {

	/**
	 * 反射Dao层Class的相应设置
	 * 
	 * @param daoClass
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T extends IBaseDao> EntityDaoWarp reflectEntityDaoWarp(Class<T> daoClass) {
		EntityDaoWarp<T> warp = new EntityDaoWarp<T>();
		warp.setMapperClass((Class<T>) ClassUtils.getSuperClassGenricType(daoClass, 0));
		warp.setEntityClass((Class<T>) ClassUtils.getSuperClassGenricType(daoClass, 1));
		warp.setPkClass((Class<T>) ClassUtils.getSuperClassGenricType(daoClass, 2));
		return warp;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T extends IBaseDao> EntityDaoWarp reflectEntityBaseDaoWarp(Class<T> daoClass) {
		EntityDaoWarp<T> warp = new EntityDaoWarp<T>();
		warp.setEntityClass((Class<T>) ClassUtils.getSuperClassGenricType(daoClass, 0));
		warp.setPkClass((Class<T>) ClassUtils.getSuperClassGenricType(daoClass, 1));
		return warp;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T extends IBaseDao> EntityDaoWarp reflectRepositoryBaseDaoWarp(Class<T> daoClass) {
		EntityDaoWarp<T> warp = new EntityDaoWarp<T>();
		warp.setRepositoryClass((Class<T>) ClassUtils.getSuperClassGenricType(daoClass, 0));
		warp.setEntityClass((Class<T>) ClassUtils.getSuperClassGenricType(daoClass, 1));
		warp.setPkClass((Class<T>) ClassUtils.getSuperClassGenricType(daoClass, 2));
		return warp;
	}
}
