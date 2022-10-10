package com.jbm.framework.masterdata.controller;

import com.jbm.framework.form.ObjectIdsForm;
import com.jbm.framework.masterdata.usage.entity.MultiPlatformEntity;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.form.EntityPageSearchForm;
import com.jbm.framework.usage.form.EntityRequsetForm;
import com.jbm.framework.usage.paging.DataPaging;

import java.util.List;

public interface IMultiPlatformController<Entity extends MultiPlatformEntity> {


    /**
     * 查询列表
     *
     * @param entityPageSearchForm
     * @return
     */
    ResultBody<DataPaging<Entity>> pageList(EntityPageSearchForm<Entity> entityPageSearchForm);

    ResultBody<List<Entity>> list(EntityRequsetForm<Entity> entityRequsetForm);

    /**
     * 获取单一对对象
     *
     * @param entityRequsetForm
     * @return
     */
    ResultBody<Entity> model(EntityRequsetForm<Entity> entityRequsetForm);

    /**
     * 保存单一对象
     *
     * @param entityRequsetForm
     * @return
     */
    ResultBody<Entity> save(EntityRequsetForm<Entity> entityRequsetForm);

    /**
     * @param entityRequsetForm
     * @return
     */
    ResultBody<Boolean> remove(EntityRequsetForm<Entity> entityRequsetForm);

    /**
     * 保存多个对象
     *
     * @param entityRequsetForm
     * @return
     */
    ResultBody<List<Entity>> saveBatch(EntityRequsetForm<Entity> entityRequsetForm);

    /**
     * 生产假数据
     *
     * @return
     */
    ResultBody<Entity> mock();

    /**
     * 批量删除
     *
     * @param objectIdsForm
     * @return
     */
    ResultBody<Boolean> deleteByIds(ObjectIdsForm objectIdsForm);
}
