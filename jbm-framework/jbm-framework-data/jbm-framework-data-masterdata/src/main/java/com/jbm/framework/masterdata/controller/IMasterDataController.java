package com.jbm.framework.masterdata.controller;

import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;

public interface IMasterDataController<Entity extends MasterDataEntity, Service extends IMasterDataService<Entity>> {


    /**
     * 查询列表
     *
     * @param pageRequestBody
     * @return
     */
    Object pageList(PageRequestBody pageRequestBody);

    Object list(PageRequestBody pageRequestBody);

    /**
     * 获取单一对对象
     *
     * @param pageRequestBody
     * @return
     */
    Object model(PageRequestBody pageRequestBody);

    /**
     * 保存单一对象
     *
     * @param pageRequestBody
     * @return
     */
    Object save(PageRequestBody pageRequestBody);

    /**
     * @param pageRequestBody
     * @return
     */
    Object remove(PageRequestBody pageRequestBody);

    /**
     * 保存多个对象
     *
     * @param pageRequestBody
     * @return
     */
    Object saveBatch(PageRequestBody pageRequestBody);

    /**
     * 生产假数据
     *
     * @return
     */
    Object mock();

    /**
     * 批量删除
     *
     * @param pageRequestBody
     * @return
     */
    Object deleteByIds(PageRequestBody pageRequestBody);
}
