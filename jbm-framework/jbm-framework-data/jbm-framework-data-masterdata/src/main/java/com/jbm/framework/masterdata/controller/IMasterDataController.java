package com.jbm.framework.masterdata.controller;

import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.masterdata.usage.bean.MasterDataEntity;
import com.jbm.framework.usage.form.JsonRequestBody;

public interface IMasterDataController<Entity extends MasterDataEntity, Service extends IMasterDataService<Entity>> {


    Object pageList(JsonRequestBody jsonRequestBody);

    Object model(JsonRequestBody jsonRequestBody);

    Object save(JsonRequestBody jsonRequestBody);

    Object remove(JsonRequestBody jsonRequestBody);

    Object saveBatch(JsonRequestBody jsonRequestBody);

    Object mock();

}
