package com.jbm.framework.masterdata.service;

import java.util.List;

import com.jbm.framework.exceptions.DataServiceException;
import com.jbm.framework.masterdata.usage.bean.MasterDataTreeEntity;
import com.jbm.framework.usage.paging.PageForm;

/**
 * 带有树形结构的数据库访问类
 *
 * @param <Entity>
 * @author wesley
 */
public interface IMasterDataTreeService<Entity extends MasterDataTreeEntity> extends IMasterDataService<Entity> {

    /**
     * 获取parentId为空的列表
     *
     * @param entity
     * @return
     * @throws DataServiceException
     */
    List<Entity> selectRootListById(Entity entity) throws DataServiceException;

    List<Entity> selectRootListById() throws DataServiceException;

    /**
     * 通过父节点获取所有下面的子节点（递归慎用）
     *
     * @param entity
     * @return
     * @throws DataServiceException
     */
    List<Entity> selectChildNodesById(Entity entity) throws DataServiceException;

    List<Entity> selectChildNodesById(Long parentId) throws DataServiceException;

    List<Entity> selectChildNodesByCode(String parentCode) throws DataServiceException;

    /**
     * 通过所有父节点获取下面所有子节点（递归慎用）
     *
     * @param subEntitys
     * @return
     * @throws DataServiceException
     */
    List<Entity> selectChildNodesById(List<Entity> subEntitys) throws DataServiceException;

    /**
     * 通过父节点获取子节点
     *
     * @param parentId
     * @return
     * @throws DataServiceException
     */
    List<Entity> selectListByParentId(Long parentId) throws DataServiceException;

    /**
     * 获取parentCode为空的列表
     *
     * @param entity
     * @return
     * @throws DataServiceException
     */
    List<Entity> selectRootListByCode(Entity entity) throws DataServiceException;

    List<Entity> selectRootListByCode() throws DataServiceException;

    /**
     * 通过父节点获取所有下面的子节点（递归慎用）
     *
     * @param entity
     * @return
     * @throws DataServiceException
     */
    List<Entity> selectChildNodesByCode(Entity entity) throws DataServiceException;

    /**
     * 通过所有父节点获取下面所有子节点（递归慎用）
     *
     * @param subEntitys
     * @return
     * @throws DataServiceException
     */
    List<Entity> selectChildNodesByCode(List<Entity> subEntitys) throws DataServiceException;

    /**
     * 通过父节点获取子节点
     *
     * @param parentCode
     * @return
     * @throws DataServiceException
     */
    List<Entity> selectListByParentCode(String parentCode) throws DataServiceException;


    List<Entity> selectListByParentId(Entity entity) throws DataServiceException;
}
