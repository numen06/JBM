package com.jbm.framework.masterdata.controller;

import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.masterdata.usage.entity.MasterDataTreeEntity;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;

/**
 * 带有树形结构的数据库访问类
 *
 * @param <Entity>
 * @author wesley
 */
public interface IMasterDataTreeController<Entity extends MasterDataTreeEntity, Service extends IMasterDataTreeService<Entity>> extends IMasterDataController<Entity, Service> {

    Object root(PageRequestBody pageRequestBody);

    Object tree(PageRequestBody pageRequestBody);

}
