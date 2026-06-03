package com.jbm.cluster.common.mysql.service;

import com.jbm.cluster.api.entitys.basic.PublishedApiDoc;
import com.jbm.cluster.api.model.api.OpenApiPublishRequest;
import com.jbm.framework.masterdata.service.IMasterDataService;

import java.util.List;

public interface PublishedApiDocService extends IMasterDataService<PublishedApiDoc> {

    List<PublishedApiDoc> listActive();

    PublishedApiDoc getByDocKey(String docKey);

    PublishedApiDoc publish(OpenApiPublishRequest request, Long publisherUserId);
}
