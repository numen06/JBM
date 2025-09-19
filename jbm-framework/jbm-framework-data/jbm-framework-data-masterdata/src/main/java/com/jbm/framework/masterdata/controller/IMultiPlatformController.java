package com.jbm.framework.masterdata.controller;

import com.jbm.framework.form.ObjectIdsForm;
import com.jbm.framework.masterdata.usage.entity.MultiPlatformEntity;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.form.EntityPageSearchForm;
import com.jbm.framework.usage.form.EntityRequestForm;
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

    ResultBody<List<Entity>> list(EntityRequestForm<Entity> entityRequestForm);

    /**
     * 获取单一对对象
     *
     * @param entityRequestForm
     * @return
     */
    ResultBody<Entity> model(EntityRequestForm<Entity> entityRequestForm);

    /**
     * 保存单一对象
     *
     * @param entityRequestForm
     * @return
     */
    ResultBody<Entity> save(EntityRequestForm<Entity> entityRequestForm);

    /**
     * @param entityRequestForm
     * @return
     */
    ResultBody<Boolean> remove(EntityRequestForm<Entity> entityRequestForm);

    /**
     * 保存多个对象
     *
     * @param entityRequestForm
     * @return
     */
    ResultBody<List<Entity>> saveBatch(EntityRequestForm<Entity> entityRequestForm);

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
