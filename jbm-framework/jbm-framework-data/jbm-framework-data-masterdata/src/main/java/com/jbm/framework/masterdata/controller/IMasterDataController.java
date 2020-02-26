package com.jbm.framework.masterdata.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.form.BaseRequsetBody;
import com.jbm.framework.usage.paging.DataPaging;

import java.util.List;

public interface IMasterDataController<Entity extends MasterDataEntity, Service extends IMasterDataService<Entity>> {


    /**
     * 查询列表
     *
     * @param pageRequestBody
     * @return
     */
    ResultBody<DataPaging<Entity>> pageList(PageRequestBody pageRequestBody);

    ResultBody<List<Entity>> list(BaseRequsetBody pageRequestBody);

    /**
     * 获取单一对对象
     *
     * @param pageRequestBody
     * @return
     */
    ResultBody<Entity> model(BaseRequsetBody pageRequestBody);

    /**
     * 保存单一对象
     *
     * @param pageRequestBody
     * @return
     */
    ResultBody<Entity> save(BaseRequsetBody pageRequestBody);

    /**
     * @param pageRequestBody
     * @return
     */
    ResultBody<Boolean> remove(BaseRequsetBody pageRequestBody);

    /**
     * 保存多个对象
     *
     * @param pageRequestBody
     * @return
     */
    ResultBody<List<Entity>> saveBatch(BaseRequsetBody pageRequestBody);

    /**
     * 生产假数据
     *
     * @return
     */
    ResultBody<Entity> mock();

    /**
     * 批量删除
     *
     * @param pageRequestBody
     * @return
     */
    ResultBody<Boolean> deleteByIds(BaseRequsetBody pageRequestBody);
}
