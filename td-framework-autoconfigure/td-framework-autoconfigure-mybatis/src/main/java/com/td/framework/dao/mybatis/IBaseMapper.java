package com.td.framework.dao.mybatis;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.RowBounds;

public interface IBaseMapper<Entity extends Serializable, PK> {

	/**
	 * 查询列表数据
	 * 
	 * @param entity
	 * @return
	 */
	public List<Entity> selectList(Entity entity);

	/**
	 * 查询列表数据
	 * 
	 * @param entity
	 * @return
	 */
	public <K, V> List<Entity> selectList(Map<K, V> map);

	/**
	 * 通过Map查找分页
	 * 
	 * @param map
	 * @param rowBounds
	 * @return
	 */
	public <K, V> List<Entity> selectPagingList(Map<K, V> map, RowBounds rowBounds);

	/**
	 * 通过Map查找分页
	 * 
	 * @param map
	 * @param rowBounds
	 * @return
	 */
	public <K, V> List<Entity> selectPagingList(Entity entity, RowBounds rowBounds);

	/**
	 * 通过主键删除实体
	 * 
	 * @param id
	 * @return
	 */
	public int deleteByPrimaryKey(PK id);

	/**
	 * 通过实体查询并删除实体
	 * 
	 * @param entity
	 * @return
	 */
	public int delete(Entity entity);

	/**
	 * 实体插入
	 * 
	 * @param entity
	 * @return
	 */
	public int insert(Entity entity);

	/**
	 * 批量插入
	 * 
	 * @param entitys
	 * @return
	 */
	public int insertBatch(Collection<Entity> entitys);

	/**
	 * 实体选择属性插入
	 * 
	 * @param entity
	 * @return
	 */
	public int insertSelective(Entity entity);

	/**
	 * 通过主键查询为实体
	 * 
	 * @param id
	 * @return
	 */
	public Entity selectByPrimaryKey(PK id);

	/**
	 * 判断属性更新实体
	 * 
	 * @param entity
	 * @return
	 */
	public int updateByPrimaryKeySelective(Entity entity);

	/**
	 * 通过主键更新实体
	 * 
	 * @param entity
	 * @return
	 */
	public int updateByPrimaryKey(Entity entity);
}