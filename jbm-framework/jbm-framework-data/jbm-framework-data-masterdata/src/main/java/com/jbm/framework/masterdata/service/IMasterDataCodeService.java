package com.jbm.framework.masterdata.service;

import com.jbm.framework.exceptions.DataServiceException;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.usage.paging.DataPaging;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IMasterDataCodeService<Entity extends MasterDataEntity> extends IMasterDataService<Entity> {

    public final static String CODE_COLUMN = "code";


    /**
     * 分页查询
     *
     * @param pageRequestBody
     * @return
     */
    DataPaging<Entity> findListPage(PageRequestBody pageRequestBody);

    /**
     * 通过code删除实体
     *
     * @param code
     * @return
     * @throws DataServiceException
     */
    boolean deleteByCode(String code) throws DataServiceException;

    /**
     * 通过code查询实体
     *
     * @param code
     * @return
     * @throws DataServiceException
     */
    Entity selectByCode(String code) throws DataServiceException;

    /**
     * 通过code更新多个实体
     *
     * @param entityList
     * @return
     */
    boolean updateBatchByCode(Collection<Entity> entityList);

    /**
     * 通过code添加或者更新实体
     *
     * @param entityList
     * @return
     */
    boolean saveOrUpdateBatchByCode(Collection<Entity> entityList);

    /**
     * 通过多个code删除实体
     *
     * @param codes
     * @return
     * @throws DataServiceException
     */
    int deleteByCodes(Collection<String> codes) throws DataServiceException;

    boolean updateByCode(Entity entity);

    List<Entity> selectByCodes(Collection<String> codes) throws DataServiceException;

    Map<String, Entity> selectEntityMapByCodes(Collection<String> codes) throws DataServiceException;


}
