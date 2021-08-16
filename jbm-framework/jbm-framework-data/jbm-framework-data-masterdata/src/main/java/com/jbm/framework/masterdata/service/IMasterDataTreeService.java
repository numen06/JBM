package com.jbm.framework.masterdata.service;

import java.util.List;

import com.jbm.framework.exceptions.DataServiceException;
import com.jbm.framework.masterdata.usage.entity.MasterDataTreeEntity;

/**
 * 带有树形结构的数据库访问类
 *
 * @param <Entity>
 * @author wesley
 */
public interface IMasterDataTreeService<Entity extends MasterDataTreeEntity> extends IMasterDataService<Entity> {

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


    List<Entity> selectListByParentId(Entity entity) throws DataServiceException;
}
