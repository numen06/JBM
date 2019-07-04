package com.jbm.framework.masterdata.controller;

import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.bean.MasterDataEntity;
import com.jbm.framework.usage.form.JsonRequestBody;

public interface IMasterDataController<Entity extends MasterDataEntity, Service extends IMasterDataService<Entity>> {


    /**
     * 查询列表
     *
     * @param jsonRequestBody
     * @return
     */
    Object pageList(JsonRequestBody jsonRequestBody);

    /**
     * 获取单一对对象
     * @param jsonRequestBody
     * @return
     */
    Object model(JsonRequestBody jsonRequestBody);

    /**
     * 保存单一对象
     * @param jsonRequestBody
     * @return
     */
    Object save(JsonRequestBody jsonRequestBody);

    /**
     * @param jsonRequestBody
     * @return
     */
    Object remove(JsonRequestBody jsonRequestBody);

    /**
     * 保存多个对象
     * @param jsonRequestBody
     * @return
     */
    Object saveBatch(JsonRequestBody jsonRequestBody);

    /**
     * 生产假数据
     * @return
     */
    Object mock();

    /**
     * 批量删除
     * @param jsonRequestBody
     * @return
     */
    Object deleteByIds(JsonRequestBody jsonRequestBody);
}
