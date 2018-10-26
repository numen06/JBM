package com.jbm.framework.service;

import java.io.Serializable;
import java.util.List;

import com.jbm.framework.metadata.exceptions.DataServiceException;
import com.jbm.masterdata.entity.common.MasterLevelEntity;

/**
 * 带有树形结构的数据库访问类
 * 
 * @author wesley
 *
 * @param <Entity>
 * @param <PK>
 */
public interface ILevelSqlService<Entity extends MasterLevelEntity<CODE>, CODE extends Serializable> extends IAdvSqlService<Entity, Long> {

	/**
	 * 获取parentCode为空的列表
	 * 
	 * @param entity
	 * @return
	 * @throws DataServiceException
	 */
	List<Entity> selectRootCodeList(Entity entity) throws DataServiceException;
	
	List<Entity> selectRootIdList(Entity entity) throws DataServiceException;
	

	/**
	 * 通过父节点获取所有下面的子节点（递归慎用）
	 * 
	 * @param entity
	 * @return
	 * @throws DataServiceException
	 */
	List<Entity> selectTreeByParentId(Entity entity) throws DataServiceException;

	/**
	 * 通过父节点获取所有下面的子节点（递归慎用）
	 * 
	 * @param entity
	 * @return
	 * @throws DataServiceException
	 */
	List<Entity> selectListByParentId(Long id) throws DataServiceException;

	/**
	 * 通过父节点获取所有下面的子节点（递归慎用）
	 * 
	 * @param entity
	 * @return
	 * @throws DataServiceException
	 */
	List<Entity> selectTreeByParentCode(Entity entity) throws DataServiceException;

	/**
	 * 通过所有父节点获取下面所有子节点（递归慎用）
	 * 
	 * @param subEntitys
	 * @return
	 * @throws DataServiceException
	 */
	List<Entity> selectTreeByParentCode(List<Entity> subEntitys) throws DataServiceException;

	/**
	 * 通过父节点获取子节点
	 * 
	 * @param parentCode
	 * @return
	 * @throws DataServiceException
	 */
	List<Entity> selectListByParentCode(CODE parentCode) throws DataServiceException;
}
