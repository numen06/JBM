package com.jbm.framework.service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.jbm.framework.metadata.usage.page.DataPaging;
import com.jbm.framework.metadata.usage.page.PageForm;

/**
 * 
 * 基于Mybatis的基础DAO接口
 * 
 * @author wesley
 *
 * @param <Entity>
 */
public interface IBaseMySqlService<Entity extends Serializable> extends IBaseService<Entity> {
	/**
	 * 保存（持久化）对象
	 * 
	 * @return 执行成功数
	 */
	public Long save(Entity entity);

	/**
	 * 插入一个实体
	 * 
	 * @param entity
	 * @return
	 */
	public Long insert(Entity entity);

	/**
	 * 删除（持久化）对象
	 * 
	 * @param entity
	 * @return
	 */
	public Integer delete(Entity entity);

	/**
	 * 更新（持久化）对象
	 * 
	 * @param entity
	 *            持久化对象
	 * @return 执行成功的记录个数
	 */
	public Long update(Entity entity);

	/**
	 * 通过实体去查询其他信息一般用于ID
	 * 
	 * @param entity
	 * @return
	 */
	public Entity selectEntity(Entity entity);

	/**
	 * 
	 * 通过Map查询实体
	 * 
	 * @param params
	 * @return
	 */
	public <K, V> Entity selectEntity(Map<K, V> params);

	/**
	 * 不分页查询
	 * 
	 * @param entity
	 * @return 查询结果
	 */
	public List<Entity> selectEntitys(Entity entity);

	/**
	 * 不分页查询
	 * 
	 * @return 查询结果
	 */
	public <K, V> List<Entity> selectEntitys(Map<K, V> params);

	/**
	 * 查询实体
	 * 
	 * @param entity
	 * @param pageable
	 * @return
	 */
	public DataPaging<Entity> selectEntitys(Entity entity, PageForm pageForm);

	/**
	 * 
	 * 分页查询实体
	 * 
	 * @param params
	 * @param pageable
	 * @return
	 */
	public <K, V> DataPaging<Entity> selectEntitys(Map<K, V> params, PageForm pageForm);

	/**
	 * 
	 * 批量插入返回最后一的插入值的ID
	 * 
	 * @param records
	 * @return
	 */
	public Long insertBatch(Collection<Entity> records);

	/**
	 * 通过实体查询分页，并且加入扩展字段
	 * 
	 * @param entity
	 *            实体
	 * @param expand
	 *            扩展字段
	 * @param pageable
	 *            分页信息
	 * @return
	 */
	DataPaging<Entity> selectEntitys(Entity entity, Map<String, Object> expand, PageForm pageForm);

	/**
	 * 通过实体查询列表，并且加入扩展
	 * 
	 * @param entity
	 *            实体
	 * @param expand
	 *            扩展
	 * @return
	 */
	List<Entity> selectEntitys(Entity entity, Map<String, Object> expand);

	/**
	 * 统计数量
	 * 
	 * @param entity
	 * @return
	 */
	Long count(Entity entity);
}
