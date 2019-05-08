package com.jbm.framework.masterdata.controller;

import com.jbm.framework.exceptions.DataServiceException;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.service.IMasterDataTreeService;
import com.jbm.framework.masterdata.usage.bean.MasterDataEntity;
import com.jbm.framework.masterdata.usage.bean.MasterDataTreeEntity;
import com.jbm.framework.usage.form.JsonRequestBody;

import java.util.List;

/**
 * 带有树形结构的数据库访问类
 *
 * @param <Entity>
 * @author wesley
 */
public interface IMasterDataTreeController<Entity extends MasterDataTreeEntity, Service extends IMasterDataTreeService<Entity>> extends IMasterDataController<Entity, Service> {

    Object root(JsonRequestBody jsonRequestBody);

    Object rootByCode(JsonRequestBody jsonRequestBody);

    Object tree(JsonRequestBody jsonRequestBody);

    Object treeByCode(JsonRequestBody jsonRequestBody);
}
