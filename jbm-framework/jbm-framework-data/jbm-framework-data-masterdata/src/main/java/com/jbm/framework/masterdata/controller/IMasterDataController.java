package com.jbm.framework.masterdata.controller;

import com.jbm.framework.form.IdsForm;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;

import java.util.List;

public interface IMasterDataController<Entity extends MasterDataEntity> {


    /**
     * 查询列表
     *
     * @param pageRequestBody
     * @return
     */
    ResultBody<DataPaging<Entity>> pageList(PageRequestBody pageRequestBody);

    ResultBody<List<Entity>> list(MasterDataRequsetBody masterDataRequsetBody);

    /**
     * 获取单一对对象
     *
     * @param masterDataRequsetBody
     * @return
     */
    ResultBody<Entity> model(MasterDataRequsetBody masterDataRequsetBody);

    /**
     * 保存单一对象
     *
     * @param masterDataRequsetBody
     * @return
     */
    ResultBody<Entity> save(MasterDataRequsetBody masterDataRequsetBody);

    /**
     * @param masterDataRequsetBody
     * @return
     */
    ResultBody<Boolean> remove(MasterDataRequsetBody masterDataRequsetBody);

    /**
     * 保存多个对象
     *
     * @param masterDataRequsetBody
     * @return
     */
    ResultBody<List<Entity>> saveBatch(MasterDataRequsetBody masterDataRequsetBody);

    /**
     * 生产假数据
     *
     * @return
     */
    ResultBody<Entity> mock();

    /**
     * 批量删除
     *
     * @param idsForm
     * @return
     */
    ResultBody<Boolean> deleteByIds(IdsForm idsForm);
}
