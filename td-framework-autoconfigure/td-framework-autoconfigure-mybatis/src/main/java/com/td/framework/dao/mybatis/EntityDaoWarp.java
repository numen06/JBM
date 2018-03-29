package com.td.framework.dao.mybatis;

/**
 * 
 * 封装业务服务的实体类型
 * 
 * @author wesley
 *
 * @param <T>
 */
public class EntityDaoWarp<T> {

	private Class<T> entityClass;

	private Class<T> mapperClass;

	private Class<T> pkClass;

	private Class<T> repositoryClass;

	public Class<T> getEntityClass() {
		return entityClass;
	}

	public void setEntityClass(Class<T> entityClass) {
		this.entityClass = entityClass;
	}

	public Class<T> getMapperClass() {
		return mapperClass;
	}

	public void setMapperClass(Class<T> mapperClass) {
		this.mapperClass = mapperClass;
	}

	public Class<T> getPkClass() {
		return pkClass;
	}

	public void setPkClass(Class<T> pkClass) {
		this.pkClass = pkClass;
	}

	public Class<T> getRepositoryClass() {
		return repositoryClass;
	}

	public void setRepositoryClass(Class<T> repositoryClass) {
		this.repositoryClass = repositoryClass;
	}

}
