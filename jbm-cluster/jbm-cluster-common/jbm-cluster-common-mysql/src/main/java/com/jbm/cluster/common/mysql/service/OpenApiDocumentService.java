package com.jbm.cluster.common.mysql.service;

import com.jbm.cluster.api.entitys.basic.OpenApiDocument;
import com.jbm.framework.masterdata.service.IMasterDataService;

public interface OpenApiDocumentService extends IMasterDataService<OpenApiDocument> {

    OpenApiDocument getByServiceId(String serviceId);
}
