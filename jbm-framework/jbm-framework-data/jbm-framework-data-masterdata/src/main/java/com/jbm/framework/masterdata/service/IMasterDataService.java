package com.jbm.framework.masterdata.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.framework.exceptions.DataServiceException;
import com.jbm.framework.masterdata.usage.CriteriaQueryWrapper;
import com.jbm.framework.masterdata.usage.PageParams;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IMasterDataService<Entity extends MasterDataEntity> extends IBaseService<Entity> {


    /**
     * 保存（持久化）对象
     *
     * @return 执行成功数
     */
    public Entity saveEntity(Entity entity);


    @Transactional(rollbackFor = Exception.class)
    boolean saveEntitys(Collection<Entity> entityList);


    /**
     * 通过实体去查询其他信息一般用于ID
     *
     * @param entity
     * @return
     */
    public Entity selectEntity(Entity entity);

    /**
     * 不分页查询
     *
     * @param entity
     * @return 查询结果
     */
    public List<Entity> selectEntitys(Entity entity);

    /**
     * 查询实体
     *
     * @param entity
     * @param pageForm
     * @return
     */
    public DataPaging<Entity> selectEntitys(Entity entity, PageForm pageForm);

    @Transactional(rollbackFor = Exception.class)
    boolean updateEntity(Entity entity, Entity updateEntity);

    /**
     * 统计数量
     *
     * @param entity
     * @return
     */
    Integer count(Entity entity);

    Map<Long, Entity> selectEntityDictionaryByWapper(CriteriaQueryWrapper criteriaQueryWrapper);

    /**
     * 通过一个查询条件查询
     *
     * @param queryWrapper
     * @return
     */
    List<Entity> selectEntitys(QueryWrapper queryWrapper);

    /**
     * 查询实体
     *
     * @param entity
     * @param def
     * @return
     */
    Entity selectEntity(Entity entity, Entity def);

    /**
     * 查询所有实体
     *
     * @return
     */
    List<Entity> selectAllEntitys();

    /**
     * 更新一个实体
     *
     * @param entity
     * @return
     */
    boolean updateEntity(Entity entity);

    boolean updateByWrapper(Entity entity, Wrapper<Entity> wrapper);


    Entity selectById(Long id);

    List<Entity> selectEntitys(Map<String, Object> params);

    DataPaging<Entity> selectEntitys(Map<String, Object> params, PageForm pageForm);

    /**
     * 查找列表，将实体的主键作为KEY输出为MAP
     *
     * @param entity
     * @return
     * @throws DataServiceException
     */
    Map<Long, Entity> selectEntityDictionary(Entity entity);


    /**
     * 通过Map条件查询一个实体
     *
     * @param parameter
     * @return
     * @throws DataServiceException
     */
    Entity selectEntity(Map<String, Object> parameter);

    /**
     * @param parameter
     * @param def
     * @return
     * @throws DataServiceException
     */
    Entity selectEntity(Map<String, Object> parameter, Entity def);


    /**
     * <p>
     * 插入（批量），该方法不适合 Oracle
     * </p>
     *
     * @param entityList 实体对象集合
     */
    default boolean saveBatch(Collection<Entity> entityList) {
        return saveBatch(entityList, 30);
    }

    /**
     * <p>
     * 插入（批量）
     * </p>
     *
     * @param entityList 实体对象集合
     * @param batchSize  插入批次数量
     */
    boolean saveBatch(Collection<Entity> entityList, int batchSize);

    @Transactional(rollbackFor = Exception.class)
    boolean insertEntitys(Collection<Entity> entityList);


    /**
     * <p>
     * 根据 ID 删除
     * </p>
     *
     * @param id 主键ID
     */
    boolean removeById(Serializable id);

    /**
     * <p>
     * 根据 columnMap 条件，删除记录
     * </p>
     *
     * @param columnMap 表字段 map 对象
     */
    boolean removeByMap(Map<String, Object> columnMap);

    /**
     * <p>
     * 删除（根据ID 批量删除）
     * </p>
     *
     * @param idList 主键ID列表
     */
    boolean removeByIds(Collection<? extends Serializable> idList);

    /**
     * <p>
     * 根据 ID 选择修改
     * </p>
     *
     * @param entity 实体对象
     */
    boolean updateById(Entity entity);

    @Transactional(rollbackFor = Exception.class)
    DataPaging<Entity> selectEntitys(PageRequestBody<Entity> pageRequestBody);

    /**
     * <p>
     * 根据ID 批量更新
     * </p>
     *
     * @param entityList 实体对象集合
     */
    default boolean updateBatchById(Collection<Entity> entityList) {
        return updateBatchById(entityList, 30);
    }

    /**
     * <p>
     * 根据ID 批量更新
     * </p>
     *
     * @param entityList 实体对象集合
     * @param batchSize  更新批次数量
     */
    boolean updateBatchById(Collection<Entity> entityList, int batchSize);


    /**
     * 插入一个实体
     *
     * @param entity
     * @return
     */
    Entity insertEntity(Entity entity);

    /**
     * 删除一个实体
     *
     * @param entity
     * @return
     */
    boolean deleteEntity(Entity entity);

    /**
     * 根据一个ID删除实体
     *
     * @param id
     * @return
     */
    boolean deleteById(Long id);

    /**
     * 根据ID数组查询
     *
     * @param ids
     * @return
     */
    List<Entity> selectByIds(Collection<Long> ids);


    /**
     * 通过包装条件查询分页
     *
     * @param criteriaQueryWrapper
     * @return
     */
    DataPaging<Entity> selectEntitysByWapper(CriteriaQueryWrapper<Entity> criteriaQueryWrapper);

    /**
     * 通过包装条件查询分页
     *
     * @param queryWrapper
     * @param pageForm
     * @return
     */
    DataPaging<Entity> selectEntitysByWapper(QueryWrapper<Entity> queryWrapper, PageForm pageForm);

    List<Entity> selectEntitysByWapper(QueryWrapper<Entity> queryWrapper);

    /**
     * 通过包装条件查询实体
     *
     * @param queryWrapper
     * @return
     */
    Entity selectEntityByWapper(QueryWrapper<Entity> queryWrapper);

    boolean deleteByWapper(QueryWrapper<Entity> queryWrapper);

    Class<Entity> currentEntityClass();

    DataPaging<Entity> selectEntitys(CriteriaQueryWrapper<Entity> wrapper);

    DataPaging<Entity> selectEntitys(PageParams pageParams, QueryWrapper queryWrapper);
}
