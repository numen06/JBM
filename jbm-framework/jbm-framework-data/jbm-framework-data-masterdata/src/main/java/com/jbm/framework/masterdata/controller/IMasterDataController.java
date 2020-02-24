package com.jbm.framework.masterdata.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.usage.form.BaseRequsetBody;

public interface IMasterDataController<Entity extends MasterDataEntity, Service extends IMasterDataService<Entity>> {


    /**
     * 查询列表
     *
     * @param pageRequestBody
     * @return
     */
    Object pageList(PageRequestBody pageRequestBody);

    Object list(BaseRequsetBody pageRequestBody);

    /**
     * 获取单一对对象
     *
     * @param pageRequestBody
     * @return
     */
    Object model(BaseRequsetBody pageRequestBody);

    /**
     * 保存单一对象
     *
     * @param pageRequestBody
     * @return
     */
    Object save(BaseRequsetBody pageRequestBody);

    /**
     * @param pageRequestBody
     * @return
     */
    Object remove(BaseRequsetBody pageRequestBody);

    /**
     * 保存多个对象
     *
     * @param pageRequestBody
     * @return
     */
    Object saveBatch(BaseRequsetBody pageRequestBody);

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
    Object deleteByIds(BaseRequsetBody pageRequestBody);
}
