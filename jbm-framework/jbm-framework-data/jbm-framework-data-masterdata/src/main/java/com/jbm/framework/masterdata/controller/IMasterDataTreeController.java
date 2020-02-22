package com.jbm.framework.masterdata.controller;

import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.masterdata.usage.entity.MasterDataTreeEntity;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.metadata.bean.ResultForm;
import com.jbm.framework.usage.form.BaseRequsetBody;

/**
 * 带有树形结构的数据库访问类
 *
 * @param <Entity>
 * @author wesley
 */
public interface IMasterDataTreeController<Entity extends MasterDataTreeEntity, Service extends IMasterDataTreeService<Entity>> extends IMasterDataController<Entity, Service> {
    ResultForm root(BaseRequsetBody baseRequsetBody);

    ResultForm tree(BaseRequsetBody baseRequsetBody);

}
