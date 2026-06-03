package com.jbm.cluster.common.mysql.service;

import com.jbm.cluster.api.entitys.basic.OpenApiOperation;
import com.jbm.cluster.api.form.OpenApiOperationForm;
import com.jbm.cluster.api.model.api.OpenApiOperationView;
import com.jbm.framework.masterdata.service.IMasterDataService;
import com.jbm.framework.usage.paging.DataPaging;

import java.util.List;

public interface OpenApiOperationService extends IMasterDataService<OpenApiOperation> {

    DataPaging<OpenApiOperationView> findOperationViews(OpenApiOperationForm form);

    OpenApiOperation getByOperationKey(String operationKey);

    List<OpenApiOperation> listByServiceId(String serviceId);

    int countByServiceId(String serviceId, String syncState);

    int countLinkedByServiceId(String serviceId);
}
